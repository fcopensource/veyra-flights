// Turns Duffel's API objects into the small, stable shapes the app uses, and checks what the app sends.

/** "PT2H35M" or "P1DT3H" → minutes. */
export function durationMinutes(iso) {
  const m = /^P(?:(\d+)D)?(?:T(?:(\d+)H)?(?:(\d+)M)?)?$/.exec(iso ?? "");
  if (!m) return 0;
  return Number(m[1] ?? 0) * 1440 + Number(m[2] ?? 0) * 60 + Number(m[3] ?? 0);
}

const place = p => ({ code: p?.iata_code ?? "", name: p?.name ?? "", city: p?.city_name ?? p?.city?.name ?? p?.name ?? "" });

/** One flight offer, as the app shows it. */
export function normalizeOffer(o) {
  const slices = (o.slices ?? []).map(s => {
    const segments = (s.segments ?? []).map(seg => ({
      flight: `${seg.marketing_carrier?.iata_code ?? ""}${seg.marketing_carrier_flight_number ?? ""}`,
      carrier: seg.marketing_carrier?.name ?? seg.operating_carrier?.name ?? "",
      carrierCode: seg.marketing_carrier?.iata_code ?? "",
      from: place(seg.origin), to: place(seg.destination),
      depart: seg.departing_at, arrive: seg.arriving_at,
      durationMinutes: durationMinutes(seg.duration),
      aircraft: seg.aircraft?.name ?? "",
    }));
    return {
      from: place(s.origin), to: place(s.destination),
      depart: segments[0]?.depart ?? "", arrive: segments.at(-1)?.arrive ?? "",
      durationMinutes: durationMinutes(s.duration) || segments.reduce((n, x) => n + x.durationMinutes, 0),
      stops: Math.max(0, segments.length - 1),
      segments,
    };
  });
  // Bags for the first passenger on the first segment are what most people want to know.
  const bags = o.slices?.[0]?.segments?.[0]?.passengers?.[0]?.baggages ?? [];
  const count = type => bags.filter(b => b.type === type).reduce((n, b) => n + (b.quantity ?? 0), 0);
  return {
    id: o.id,
    price: { amount: Number(o.total_amount), currency: o.total_currency },
    airline: { name: o.owner?.name ?? "", code: o.owner?.iata_code ?? "", logo: o.owner?.logo_symbol_url ?? "" },
    slices,
    cabin: o.slices?.[0]?.segments?.[0]?.passengers?.[0]?.cabin_class_marketing_name ?? "",
    baggage: { checked: count("checked"), carryOn: count("carry_on") },
    refundable: !!o.conditions?.refund_before_departure?.allowed,
    changeable: !!o.conditions?.change_before_departure?.allowed,
    expiresAt: o.expires_at ?? "",
    passengerIds: (o.passengers ?? []).map(p => p.id),
  };
}

export function normalizePlace(p) {
  return { code: p.iata_code, name: p.name, city: p.city_name ?? p.name, country: p.iata_country_code ?? "", type: p.type ?? "airport" };
}

export function normalizeOrder(o) {
  return {
    id: o.id,
    reference: o.booking_reference ?? "",
    createdAt: o.created_at ?? "",
    price: { amount: Number(o.total_amount), currency: o.total_currency },
    passengers: (o.passengers ?? []).map(p => ({ name: `${p.given_name ?? ""} ${p.family_name ?? ""}`.trim() })),
    offer: normalizeOffer({ ...o, id: o.id }),
  };
}

// ---------- Checking what the app sends ----------

export class BadRequest extends Error {}
const CABINS = ["economy", "premium_economy", "business", "first"];
const isDate = s => /^\d{4}-\d{2}-\d{2}$/.test(s ?? "") && !Number.isNaN(Date.parse(`${s}T00:00:00Z`));

/** A search from the app, checked and filled in. `today` is YYYY-MM-DD. */
export function checkSearch(body, today) {
  const from = String(body?.from ?? "").toUpperCase(), to = String(body?.to ?? "").toUpperCase();
  if (!/^[A-Z]{3}$/.test(from) || !/^[A-Z]{3}$/.test(to)) throw new BadRequest("Choose where you're flying from and to.");
  if (from === to) throw new BadRequest("Pick two different places.");
  if (!isDate(body.depart)) throw new BadRequest("Choose a departure date.");
  if (body.depart < today) throw new BadRequest("The departure date has passed.");
  if (body.return !== undefined && body.return !== null && body.return !== "") {
    if (!isDate(body.return)) throw new BadRequest("Choose a valid return date.");
    if (body.return < body.depart) throw new BadRequest("The return date is before the departure date.");
  }
  const adults = Number(body.adults ?? 1), children = Number(body.children ?? 0), infants = Number(body.infants ?? 0);
  for (const n of [adults, children, infants]) if (!Number.isInteger(n) || n < 0) throw new BadRequest("Check the number of passengers.");
  if (adults < 1) throw new BadRequest("At least one adult has to travel.");
  if (adults + children > 9) throw new BadRequest("Up to 9 passengers can book together.");
  if (infants > adults) throw new BadRequest("Each infant needs an adult.");
  const cabin = CABINS.includes(body.cabin) ? body.cabin : "economy";
  return { from, to, depart: body.depart, return: body.return || undefined, adults, children, infants, cabin };
}

/** Passengers for a booking, checked. */
export function checkPassengers(list, ids) {
  if (!Array.isArray(list) || list.length !== ids.length) throw new BadRequest("Add details for every passenger.");
  return list.map((p, i) => {
    const given = String(p.given ?? "").trim(), family = String(p.family ?? "").trim();
    if (!given || !family) throw new BadRequest(`Add passenger ${i + 1}'s first and last name.`);
    if (!/^[\p{L} '\-]{1,40}$/u.test(given) || !/^[\p{L} '\-]{1,40}$/u.test(family)) throw new BadRequest(`Passenger ${i + 1}'s name can only have letters, spaces, hyphens and apostrophes.`);
    if (!isDate(p.born)) throw new BadRequest(`Add passenger ${i + 1}'s date of birth.`);
    if (!["m", "f"].includes(p.gender)) throw new BadRequest(`Choose passenger ${i + 1}'s gender as on their passport.`);
    if (!["mr", "ms", "mrs", "miss", "dr"].includes(p.title)) throw new BadRequest(`Choose passenger ${i + 1}'s title.`);
    if (i === 0 || p.email) if (!/^[^@\s]+@[^@\s]+\.[^@\s]+$/.test(p.email ?? "")) throw new BadRequest(`Add a valid email for passenger ${i + 1}.`);
    if (i === 0 || p.phone) if (!/^\+\d{7,15}$/.test(String(p.phone ?? "").replace(/[\s-]/g, ""))) throw new BadRequest(`Add passenger ${i + 1}'s phone number with the country code, like +91 98765 43210.`);
    return { id: ids[i], given_name: given, family_name: family, born_on: p.born, gender: p.gender, title: p.title, email: p.email || list[0].email, phone_number: String(p.phone || list[0].phone).replace(/[\s-]/g, "") };
  });
}
