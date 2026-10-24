import { test } from "node:test";
import assert from "node:assert/strict";
import http from "node:http";
import { checkPassengers, checkSearch, durationMinutes, normalizeOffer } from "../src/normalize.js";
import { demoPlaces, demoSearch } from "../src/demo.js";
import { createHandler } from "../src/server.js";

// A trimmed offer in Duffel's real format.
const DUFFEL_OFFER = {
  id: "off_0000A", total_amount: "5432.00", total_currency: "INR", expires_at: "2026-11-01T10:00:00Z",
  owner: { name: "IndiGo", iata_code: "6E", logo_symbol_url: "https://assets.duffel.com/img/airlines/6E.svg" },
  conditions: { refund_before_departure: { allowed: false }, change_before_departure: { allowed: true } },
  passengers: [{ id: "pas_1", type: "adult" }],
  slices: [{
    duration: "PT4H10M", origin: { iata_code: "DEL", name: "Indira Gandhi International", city_name: "New Delhi" }, destination: { iata_code: "GOI", name: "Dabolim", city_name: "Goa" },
    segments: [
      { duration: "PT2H5M", departing_at: "2026-12-20T06:00:00", arriving_at: "2026-12-20T08:05:00", marketing_carrier: { iata_code: "6E", name: "IndiGo" }, marketing_carrier_flight_number: "2041", origin: { iata_code: "DEL", city_name: "New Delhi" }, destination: { iata_code: "BOM", city_name: "Mumbai" }, aircraft: { name: "Airbus A320neo" },
        passengers: [{ cabin_class_marketing_name: "Economy", baggages: [{ type: "checked", quantity: 1 }, { type: "carry_on", quantity: 1 }] }] },
      { duration: "PT1H10M", departing_at: "2026-12-20T09:00:00", arriving_at: "2026-12-20T10:10:00", marketing_carrier: { iata_code: "6E", name: "IndiGo" }, marketing_carrier_flight_number: "511", origin: { iata_code: "BOM", city_name: "Mumbai" }, destination: { iata_code: "GOI", city_name: "Goa" } },
    ],
  }],
};

test("durations", () => {
  assert.equal(durationMinutes("PT2H35M"), 155);
  assert.equal(durationMinutes("PT45M"), 45);
  assert.equal(durationMinutes("P1DT2H"), 1560);
  assert.equal(durationMinutes("nonsense"), 0);
});

test("Duffel offers become the app's shape", () => {
  const o = normalizeOffer(DUFFEL_OFFER);
  assert.deepEqual(o.price, { amount: 5432, currency: "INR" });
  assert.equal(o.airline.code, "6E");
  assert.equal(o.slices[0].stops, 1);
  assert.equal(o.slices[0].durationMinutes, 250);
  assert.equal(o.slices[0].segments[0].flight, "6E2041");
  assert.equal(o.slices[0].segments[1].to.city, "Goa");
  assert.deepEqual(o.baggage, { checked: 1, carryOn: 1 });
  assert.equal(o.refundable, false);
  assert.equal(o.changeable, true);
  assert.deepEqual(o.passengerIds, ["pas_1"]);
});

test("searches are checked", () => {
  const today = "2026-10-24";
  const ok = checkSearch({ from: "del", to: "goi", depart: "2026-12-20", adults: 2, cabin: "business" }, today);
  assert.deepEqual(ok, { from: "DEL", to: "GOI", depart: "2026-12-20", return: undefined, adults: 2, children: 0, infants: 0, cabin: "business" });
  assert.equal(checkSearch({ from: "DEL", to: "GOI", depart: "2026-12-20", cabin: "spaceship" }, today).cabin, "economy");
  const bad = [
    [{ from: "DE", to: "GOI", depart: "2026-12-20" }, /from and to/],
    [{ from: "DEL", to: "DEL", depart: "2026-12-20" }, /different/],
    [{ from: "DEL", to: "GOI", depart: "2026-01-01" }, /passed/],
    [{ from: "DEL", to: "GOI", depart: "2026-12-20", return: "2026-12-01" }, /before/],
    [{ from: "DEL", to: "GOI", depart: "2026-12-20", adults: 0 }, /adult/],
    [{ from: "DEL", to: "GOI", depart: "2026-12-20", adults: 7, children: 3 }, /9 passengers/],
    [{ from: "DEL", to: "GOI", depart: "2026-12-20", adults: 1, infants: 2 }, /infant/],
    [{ from: "DEL", to: "GOI", depart: "2026-13-45" }, /date/],
  ];
  for (const [body, re] of bad) assert.throws(() => checkSearch(body, today), re, JSON.stringify(body));
});

test("passengers are checked", () => {
  const good = { title: "ms", given: "Anika", family: "Rao", born: "1994-05-02", gender: "f", email: "anika@example.com", phone: "+91 98765 43210" };
  const [p] = checkPassengers([good], ["pas_1"]);
  assert.deepEqual(p, { id: "pas_1", given_name: "Anika", family_name: "Rao", born_on: "1994-05-02", gender: "f", title: "ms", email: "anika@example.com", phone_number: "+919876543210" });
  const second = checkPassengers([good, { title: "mr", given: "Dev", family: "Rao", born: "2016-01-01", gender: "m" }], ["a", "b"])[1];
  assert.equal(second.email, "anika@example.com", "later passengers share the lead's contact details");
  assert.throws(() => checkPassengers([good], ["a", "b"]), /every passenger/);
  assert.throws(() => checkPassengers([{ ...good, given: "" }], ["a"]), /first and last name/);
  assert.throws(() => checkPassengers([{ ...good, given: "R2D2" }], ["a"]), /letters/);
  assert.throws(() => checkPassengers([{ ...good, email: "nope" }], ["a"]), /email/);
  assert.throws(() => checkPassengers([{ ...good, phone: "98765" }], ["a"]), /country code/);
  assert.equal(checkPassengers([{ ...good, given: "José-María" }], ["a"])[0].given_name, "José-María", "accents are fine");
});

test("demo places and flights", () => {
  assert.equal(demoPlaces("del")[0].code, "DEL");
  assert.equal(demoPlaces("goa").length, 2);
  assert.equal(demoPlaces("mum")[0].city, "Mumbai");
  assert.deepEqual(demoPlaces(""), []);
  const s = { from: "DEL", to: "GOI", depart: "2026-12-20", adults: 1, children: 0, infants: 0, cabin: "economy" };
  const a = demoSearch(s), b = demoSearch(s);
  assert.ok(a.length >= 8);
  assert.deepEqual(a.map(o => o.price.amount), b.map(o => o.price.amount), "the same search gives the same flights");
  assert.ok(a.every(o => o.demo && o.price.currency === "INR" && o.slices[0].durationMinutes > 100 && o.slices[0].durationMinutes < 600), JSON.stringify(a.map(o => o.slices[0].durationMinutes)));
  const longHaul = demoSearch({ ...s, to: "LHR" });
  assert.ok(longHaul.every(o => o.slices[0].durationMinutes > 500), "Delhi to London takes most of a day");
  assert.ok(demoSearch({ ...s, return: "2026-12-27" }).every(o => o.slices.length === 2));
  const business = demoSearch({ ...s, cabin: "business" });
  assert.ok(Math.min(...business.map(o => o.price.amount)) > Math.min(...a.map(o => o.price.amount)), "business costs more");
});

// ---------- The HTTP API ----------

async function serve(env, opts) {
  const server = http.createServer(createHandler(env, opts));
  await new Promise(r => server.listen(0, r));
  const base = `http://127.0.0.1:${server.address().port}`;
  const call = async (method, path, body, headers = {}) => {
    const res = await fetch(base + path, { method, headers: { "Content-Type": "application/json", ...headers }, body: body ? JSON.stringify(body) : undefined });
    return { status: res.status, body: await res.json() };
  };
  return { call, close: () => server.close() };
}
const now = () => new Date("2026-10-24T10:00:00Z");
const passenger = { title: "mr", given: "Arjun", family: "Mehta", born: "1990-03-14", gender: "m", email: "arjun@example.com", phone: "+919812345678" };

test("demo mode: search, look at a fare, book it, and find the booking", async () => {
  const { call, close } = await serve({}, { now });
  try {
    assert.deepEqual((await call("GET", "/health")).body, { ok: true, mode: "demo", version: "1.0.0" });
    assert.equal((await call("GET", "/api/places?q=ben")).body.places[0].code, "BLR");
    const search = await call("POST", "/api/search", { from: "BLR", to: "DEL", depart: "2026-11-10", adults: 1 });
    assert.equal(search.status, 200);
    assert.equal(search.body.mode, "demo");
    const offer = search.body.offers[0];
    assert.equal((await call("GET", `/api/offers/${offer.id}`)).body.offer.id, offer.id);
    const bad = await call("POST", "/api/bookings", { offerId: offer.id, passengers: [{ ...passenger, email: "x" }] });
    assert.equal(bad.status, 400);
    assert.match(bad.body.error, /email/);
    const booked = await call("POST", "/api/bookings", { offerId: offer.id, passengers: [passenger] });
    assert.equal(booked.status, 201);
    assert.match(booked.body.booking.reference, /^[A-Z0-9]{6}$/);
    assert.deepEqual(booked.body.booking.passengers, [{ name: "Arjun Mehta" }]);
    assert.equal((await call("GET", `/api/bookings/${booked.body.booking.id}`)).body.booking.reference, booked.body.booking.reference);
    assert.equal((await call("GET", "/api/offers/demo_nope")).status, 404);
    assert.equal((await call("POST", "/api/search", { from: "BLR", to: "BLR", depart: "2026-11-10" })).status, 400);
  } finally { close(); }
});

test("live bookings stay off until turned on on purpose", async () => {
  const { call, close } = await serve({ DUFFEL_TOKEN: "duffel_live_xyz" }, { fetchImpl: async () => new Response("{}", { status: 500 }), now });
  try {
    assert.equal((await call("GET", "/health")).body.mode, "live");
    const r = await call("POST", "/api/bookings", { offerId: "off_1", passengers: [] });
    assert.equal(r.status, 403);
    assert.match(r.body.error, /isn't open yet/);
  } finally { close(); }
});

test("an app key, when set, is required", async () => {
  const { call, close } = await serve({ APP_KEY: "secret-123" }, { now });
  try {
    assert.equal((await call("GET", "/api/places?q=del")).status, 401);
    assert.equal((await call("GET", "/api/places?q=del", undefined, { "X-App-Key": "secret-123" })).status, 200);
    assert.equal((await call("GET", "/health")).status, 200, "health stays open");
  } finally { close(); }
});

test("live mode talks to Duffel with the right headers and shapes", async () => {
  const calls = [];
  const fakeFetch = async (url, init) => {
    calls.push({ url, init });
    const path = new URL(url).pathname;
    const reply = data => new Response(JSON.stringify({ data }), { status: 200, headers: { "Content-Type": "application/json" } });
    if (path === "/places/suggestions") return reply([{ type: "airport", iata_code: "GOI", name: "Dabolim", city_name: "Goa", iata_country_code: "IN" }]);
    if (path === "/air/offer_requests") return reply({ offers: [DUFFEL_OFFER] });
    if (path === "/air/offers/off_0000A") return reply(DUFFEL_OFFER);
    if (path === "/air/orders") return reply({ id: "ord_1", booking_reference: "QX7P2L", total_amount: "5432.00", total_currency: "INR", passengers: [{ given_name: "Arjun", family_name: "Mehta" }], slices: DUFFEL_OFFER.slices, owner: DUFFEL_OFFER.owner });
    if (path === "/air/offers/off_gone") return new Response(JSON.stringify({ errors: [{ code: "offer_no_longer_available" }] }), { status: 422 });
    return new Response("{}", { status: 404 });
  };
  const { call, close } = await serve({ DUFFEL_TOKEN: "duffel_test_abc" }, { fetchImpl: fakeFetch, now });
  try {
    assert.equal((await call("GET", "/health")).body.mode, "test");
    assert.equal((await call("GET", "/api/places?q=goa")).body.places[0].code, "GOI");
    const search = await call("POST", "/api/search", { from: "DEL", to: "GOI", depart: "2026-12-20", adults: 1, children: 1 });
    assert.equal(search.body.offers[0].slices[0].stops, 1);
    const request = calls.find(c => c.url.includes("offer_requests"));
    assert.equal(request.init.headers.Authorization, "Bearer duffel_test_abc");
    assert.equal(request.init.headers["Duffel-Version"], "v2");
    assert.deepEqual(JSON.parse(request.init.body).data.passengers, [{ type: "adult" }, { age: 8 }]);
    const booked = await call("POST", "/api/bookings", { offerId: "off_0000A", passengers: [passenger] });
    assert.equal(booked.status, 201);
    assert.equal(booked.body.booking.reference, "QX7P2L");
    const order = JSON.parse(calls.find(c => c.url.endsWith("/air/orders")).init.body).data;
    assert.deepEqual(order.payments, [{ type: "balance", currency: "INR", amount: "5432.00" }], "pays the latest price");
    assert.equal(order.passengers[0].id, "pas_1");
    const gone = await call("GET", "/api/offers/off_gone");
    assert.equal(gone.status, 400);
    assert.match(gone.body.error, /no longer available/);
  } finally { close(); }
});
