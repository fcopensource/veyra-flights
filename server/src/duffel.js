// A small Duffel API client: live flights, offers and bookings. The token stays on the server.
import { normalizeOffer, normalizeOrder, normalizePlace } from "./normalize.js";

export class DuffelError extends Error {
  constructor(message, status) { super(message); this.status = status; }
}

/** What to tell the person when Duffel says no. */
function friendly(status, errors) {
  const first = errors?.[0] ?? {};
  if (status === 401 || status === 403) return "The flight service rejected the server's key. Check DUFFEL_TOKEN in server/.env.";
  if (first.code === "offer_no_longer_available" || first.code === "offer_expired") return "That fare is no longer available. Search again for the latest prices.";
  if (first.code === "price_changed") return "The price changed. Search again to see the new fare.";
  if (status === 422) return first.message || "Some details weren't accepted. Check them and try again.";
  if (status === 429) return "Too many searches right now. Wait a moment and try again.";
  return "The flight service didn't answer. Try again in a moment.";
}

export function duffel(token, fetchImpl = fetch) {
  async function call(method, path, body) {
    const res = await fetchImpl(`https://api.duffel.com${path}`, {
      method,
      headers: { Authorization: `Bearer ${token}`, "Duffel-Version": "v2", Accept: "application/json", "Content-Type": "application/json" },
      body: body ? JSON.stringify({ data: body }) : undefined,
      signal: AbortSignal.timeout(45_000),
    });
    const json = await res.json().catch(() => ({}));
    if (!res.ok) throw new DuffelError(friendly(res.status, json.errors), res.status >= 500 ? 502 : res.status === 429 ? 429 : 400);
    return json.data;
  }
  return {
    async places(query) {
      const data = await call("GET", `/places/suggestions?query=${encodeURIComponent(query)}`);
      return data.filter(p => p.iata_code).slice(0, 8).map(normalizePlace);
    },
    async search(s) {
      const slices = [{ origin: s.from, destination: s.to, departure_date: s.depart }];
      if (s.return) slices.push({ origin: s.to, destination: s.from, departure_date: s.return });
      const passengers = [
        ...Array.from({ length: s.adults }, () => ({ type: "adult" })),
        ...Array.from({ length: s.children }, () => ({ age: 8 })),
        ...Array.from({ length: s.infants }, () => ({ type: "infant_without_seat" })),
      ];
      const data = await call("POST", "/air/offer_requests?return_offers=true&supplier_timeout=20000", { slices, passengers, cabin_class: s.cabin });
      return (data.offers ?? []).map(normalizeOffer);
    },
    async offer(id) { return normalizeOffer(await call("GET", `/air/offers/${encodeURIComponent(id)}`)); },
    async book(offerId, passengers) {
      // Fetch the offer again for its current price: Duffel books at the exact latest amount.
      const offer = await call("GET", `/air/offers/${encodeURIComponent(offerId)}`);
      const order = await call("POST", "/air/orders", {
        type: "instant", selected_offers: [offerId], passengers,
        payments: [{ type: "balance", currency: offer.total_currency, amount: offer.total_amount }],
      });
      return normalizeOrder(order);
    },
    async booking(id) { return normalizeOrder(await call("GET", `/air/orders/${encodeURIComponent(id)}`)); },
  };
}
