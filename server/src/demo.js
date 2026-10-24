// Demo flights for building and testing without a Duffel key. The app labels them clearly: they aren't real fares.
// Airports are real (with their real positions), so durations and prices are believable.

const AIRPORTS = [
  ["DEL", "Indira Gandhi International", "New Delhi", "IN", 28.556, 77.1], ["BOM", "Chhatrapati Shivaji Maharaj International", "Mumbai", "IN", 19.089, 72.868],
  ["BLR", "Kempegowda International", "Bengaluru", "IN", 13.198, 77.706], ["MAA", "Chennai International", "Chennai", "IN", 12.99, 80.169],
  ["HYD", "Rajiv Gandhi International", "Hyderabad", "IN", 17.24, 78.429], ["CCU", "Netaji Subhas Chandra Bose International", "Kolkata", "IN", 22.654, 88.447],
  ["GOI", "Dabolim", "Goa", "IN", 15.381, 73.831], ["GOX", "Manohar International", "Goa", "IN", 15.744, 73.86], ["COK", "Cochin International", "Kochi", "IN", 10.152, 76.402],
  ["AMD", "Sardar Vallabhbhai Patel International", "Ahmedabad", "IN", 23.077, 72.635], ["PNQ", "Pune", "Pune", "IN", 18.582, 73.92], ["JAI", "Jaipur International", "Jaipur", "IN", 26.824, 75.812],
  ["LKO", "Chaudhary Charan Singh International", "Lucknow", "IN", 26.761, 80.889], ["IXC", "Chandigarh International", "Chandigarh", "IN", 30.673, 76.788], ["SXR", "Srinagar International", "Srinagar", "IN", 33.987, 74.774],
  ["ATQ", "Sri Guru Ram Dass Jee International", "Amritsar", "IN", 31.709, 74.797], ["TRV", "Thiruvananthapuram International", "Thiruvananthapuram", "IN", 8.482, 76.92], ["VNS", "Lal Bahadur Shastri International", "Varanasi", "IN", 25.452, 82.859],
  ["DXB", "Dubai International", "Dubai", "AE", 25.253, 55.364], ["AUH", "Zayed International", "Abu Dhabi", "AE", 24.433, 54.651], ["DOH", "Hamad International", "Doha", "QA", 25.273, 51.608],
  ["SIN", "Changi", "Singapore", "SG", 1.364, 103.991], ["BKK", "Suvarnabhumi", "Bangkok", "TH", 13.69, 100.75], ["KUL", "Kuala Lumpur International", "Kuala Lumpur", "MY", 2.746, 101.71],
  ["HKG", "Hong Kong International", "Hong Kong", "HK", 22.308, 113.918], ["NRT", "Narita International", "Tokyo", "JP", 35.772, 140.393], ["HND", "Haneda", "Tokyo", "JP", 35.553, 139.78],
  ["ICN", "Incheon International", "Seoul", "KR", 37.46, 126.441], ["SYD", "Sydney Kingsford Smith", "Sydney", "AU", -33.946, 151.177], ["MEL", "Melbourne", "Melbourne", "AU", -37.669, 144.841],
  ["LHR", "Heathrow", "London", "GB", 51.47, -0.454], ["LGW", "Gatwick", "London", "GB", 51.148, -0.19], ["CDG", "Charles de Gaulle", "Paris", "FR", 49.01, 2.548],
  ["FRA", "Frankfurt", "Frankfurt", "DE", 50.033, 8.571], ["AMS", "Schiphol", "Amsterdam", "NL", 52.31, 4.768], ["IST", "Istanbul", "Istanbul", "TR", 41.262, 28.742],
  ["ZRH", "Zurich", "Zurich", "CH", 47.465, 8.549], ["MAD", "Adolfo Suárez Madrid–Barajas", "Madrid", "ES", 40.472, -3.561], ["FCO", "Leonardo da Vinci–Fiumicino", "Rome", "IT", 41.8, 12.239],
  ["JFK", "John F. Kennedy International", "New York", "US", 40.641, -73.778], ["EWR", "Newark Liberty International", "New York", "US", 40.69, -74.174], ["SFO", "San Francisco International", "San Francisco", "US", 37.62, -122.379],
  ["LAX", "Los Angeles International", "Los Angeles", "US", 33.942, -118.408], ["ORD", "O'Hare International", "Chicago", "US", 41.974, -87.907], ["YYZ", "Toronto Pearson", "Toronto", "CA", 43.677, -79.624],
  ["JNB", "O. R. Tambo International", "Johannesburg", "ZA", -26.139, 28.246], ["NBO", "Jomo Kenyatta International", "Nairobi", "KE", -1.319, 36.928], ["CMB", "Bandaranaike International", "Colombo", "LK", 7.18, 79.884],
  ["KTM", "Tribhuvan International", "Kathmandu", "NP", 27.697, 85.359], ["DAC", "Hazrat Shahjalal International", "Dhaka", "BD", 23.843, 90.398], ["MLE", "Velana International", "Malé", "MV", 4.192, 73.529],
].map(([code, name, city, country, lat, lon]) => ({ code, name, city, country, lat, lon }));
const BY_CODE = new Map(AIRPORTS.map(a => [a.code, a]));

const DOMESTIC_IN = [["6E", "IndiGo"], ["AI", "Air India"], ["IX", "Air India Express"], ["QP", "Akasa Air"], ["SG", "SpiceJet"]];
const INTERNATIONAL = [["EK", "Emirates"], ["QR", "Qatar Airways"], ["SQ", "Singapore Airlines"], ["AI", "Air India"], ["LH", "Lufthansa"], ["BA", "British Airways"], ["EY", "Etihad Airways"], ["TK", "Turkish Airlines"], ["6E", "IndiGo"]];
const HUBS = { EK: "DXB", QR: "DOH", SQ: "SIN", LH: "FRA", BA: "LHR", EY: "AUH", TK: "IST", AI: "DEL", "6E": "DEL", IX: "DEL", QP: "BOM", SG: "DEL" };

/** Airports whose code, city or name starts with or contains the query. */
export function demoPlaces(query) {
  const q = query.trim().toLowerCase();
  if (!q) return [];
  const score = a => (a.code.toLowerCase() === q ? 0 : a.city.toLowerCase().startsWith(q) ? 1 : a.code.toLowerCase().startsWith(q) ? 2 : a.name.toLowerCase().includes(q) || a.city.toLowerCase().includes(q) ? 3 : 9);
  return AIRPORTS.map(a => [score(a), a]).filter(([s]) => s < 9).sort((x, y) => x[0] - y[0] || x[1].city.localeCompare(y[1].city)).slice(0, 8)
    .map(([, a]) => ({ code: a.code, name: a.name, city: a.city, country: a.country, type: "airport" }));
}

function km(a, b) {
  const r = Math.PI / 180, dLat = (b.lat - a.lat) * r, dLon = (b.lon - a.lon) * r;
  const h = Math.sin(dLat / 2) ** 2 + Math.cos(a.lat * r) * Math.cos(b.lat * r) * Math.sin(dLon / 2) ** 2;
  return 6371 * 2 * Math.asin(Math.sqrt(h));
}
function seeded(text) {
  let h = 2166136261;
  for (const ch of text) h = Math.imul(h ^ ch.charCodeAt(0), 16777619) >>> 0;
  return () => { h = (h + 0x6d2b79f5) >>> 0; let t = h; t = Math.imul(t ^ (t >>> 15), t | 1); t ^= t + Math.imul(t ^ (t >>> 7), t | 61); return ((t ^ (t >>> 14)) >>> 0) / 4294967296; };
}
const pad = n => String(n).padStart(2, "0");
const local = (date, minutes) => { const d = new Date(`${date}T00:00:00Z`); d.setUTCMinutes(d.getUTCMinutes() + minutes); return `${d.getUTCFullYear()}-${pad(d.getUTCMonth() + 1)}-${pad(d.getUTCDate())}T${pad(d.getUTCHours())}:${pad(d.getUTCMinutes())}:00`; };
const CABIN = { economy: [1, "Economy"], premium_economy: [1.7, "Premium Economy"], business: [3.4, "Business"], first: [5.5, "First"] };
const strip = a => ({ code: a.code, name: a.name, city: a.city });

const offers = new Map();
const bookings = new Map();

function slice(rnd, from, to, date, carrier, stops, startMinute) {
  const segs = [];
  const hub = stops ? BY_CODE.get(HUBS[carrier[0]]) : undefined;
  const legs = stops && hub && hub.code !== from.code && hub.code !== to.code ? [[from, hub], [hub, to]] : [[from, to]];
  let t = startMinute;
  for (const [a, b] of legs) {
    const flying = Math.round(km(a, b) / 780 * 60 + 28 + rnd() * 12);
    segs.push({ flight: `${carrier[0]}${100 + Math.floor(rnd() * 899)}`, carrier: carrier[1], carrierCode: carrier[0], from: strip(a), to: strip(b), depart: local(date, t), arrive: local(date, t + flying), durationMinutes: flying, aircraft: km(a, b) > 3500 ? (rnd() < 0.5 ? "Boeing 787-9" : "Airbus A350-900") : (rnd() < 0.5 ? "Airbus A320neo" : "Boeing 737-8") });
    t += flying + (legs.length > 1 ? 70 + Math.floor(rnd() * 150) : 0);
  }
  return { from: strip(from), to: strip(to), depart: segs[0].depart, arrive: segs.at(-1).arrive, durationMinutes: t - startMinute, stops: segs.length - 1, segments: segs };
}

/** Believable offers for a route and date, the same every time for the same search. */
export function demoSearch(s) {
  const from = BY_CODE.get(s.from), to = BY_CODE.get(s.to);
  if (!from || !to) return [];
  const rnd = seeded(`${s.from}${s.to}${s.depart}${s.return ?? ""}${s.cabin}`);
  const domestic = from.country === "IN" && to.country === "IN";
  const carriers = domestic ? DOMESTIC_IN : INTERNATIONAL;
  const distance = km(from, to);
  const inr = from.country === "IN";
  const [cabinFactor, cabinName] = CABIN[s.cabin] ?? CABIN.economy;
  const payers = s.adults + s.children * 0.75 + s.infants * 0.1;
  const count = 8 + Math.floor(rnd() * 8);
  const list = [];
  for (let i = 0; i < count; i++) {
    const carrier = carriers[Math.floor(rnd() * carriers.length)];
    const stops = distance > 2200 && rnd() < 0.55 ? 1 : distance > 900 && rnd() < 0.2 ? 1 : 0;
    const start = 5 * 60 + Math.floor(rnd() * 17 * 60 / 5) * 5;
    const out = slice(rnd, from, to, s.depart, carrier, stops, start);
    const back = s.return ? slice(rnd, to, from, s.return, carrier, stops, 6 * 60 + Math.floor(rnd() * 15 * 60 / 5) * 5) : undefined;
    const usd = (35 + distance * 0.075 * (0.8 + rnd() * 0.7) - stops * 25) * cabinFactor * (back ? 1.85 : 1) * payers;
    const amount = inr ? Math.round(usd * 84 / 10) * 10 : Math.round(usd * 100) / 100;
    const id = `demo_${s.from}${s.to}_${s.depart.replace(/-/g, "")}_${i}_${Math.floor(rnd() * 1e6).toString(36)}`;
    const offer = {
      id, price: { amount, currency: inr ? "INR" : "USD" },
      airline: { name: carrier[1], code: carrier[0], logo: "" },
      slices: back ? [out, back] : [out],
      cabin: cabinName,
      baggage: { checked: domestic && s.cabin === "economy" ? (rnd() < 0.75 ? 1 : 0) : 1 + (s.cabin === "business" || s.cabin === "first" ? 1 : 0), carryOn: 1 },
      refundable: rnd() < 0.35 || s.cabin !== "economy", changeable: rnd() < 0.7,
      expiresAt: new Date(Date.now() + 30 * 60_000).toISOString(),
      passengerIds: Array.from({ length: s.adults + s.children + s.infants }, (_, k) => `pas_demo_${k + 1}`),
      demo: true,
    };
    offers.set(id, offer);
    list.push(offer);
  }
  return list;
}

export const demoOffer = id => offers.get(id);

export function demoBook(offerId, passengers) {
  const offer = offers.get(offerId);
  if (!offer) return undefined;
  const rnd = seeded(offerId + passengers.map(p => p.family_name).join());
  const letters = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
  const reference = Array.from({ length: 6 }, () => letters[Math.floor(rnd() * letters.length)]).join("");
  const booking = { id: `ord_demo_${reference.toLowerCase()}`, reference, createdAt: new Date().toISOString(), price: offer.price, passengers: passengers.map(p => ({ name: `${p.given_name} ${p.family_name}` })), offer, demo: true };
  bookings.set(booking.id, booking);
  return booking;
}
export const demoBooking = id => bookings.get(id);
