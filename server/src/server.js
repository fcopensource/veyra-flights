// Veyra Flights server: the app talks to this, and this talks to Duffel (or serves demo flights when there's no key).
//   GET  /health                 { ok, mode }
//   GET  /api/places?q=del       airports and cities
//   POST /api/search             { from, to, depart, return?, adults, children, infants, cabin } → { mode, offers }
//   GET  /api/offers/:id         one offer, with its latest price
//   POST /api/bookings           { offerId, passengers } → { booking }
//   GET  /api/bookings/:id       a booking
import http from "node:http";
import { BadRequest, checkPassengers, checkSearch } from "./normalize.js";
import { demoBook, demoBooking, demoOffer, demoPlaces, demoSearch } from "./demo.js";
import { duffel, DuffelError } from "./duffel.js";

export const VERSION = "1.0.0";

/** Builds the request handler. `env` holds DUFFEL_TOKEN and APP_KEY; `now` is for tests. */
export function createHandler(env = process.env, { fetchImpl = fetch, now = () => new Date() } = {}) {
  const live = env.DUFFEL_TOKEN ? duffel(env.DUFFEL_TOKEN, fetchImpl) : undefined;
  const mode = live ? (env.DUFFEL_TOKEN.startsWith("duffel_live") ? "live" : "test") : "demo";
  const hits = new Map(); // ip → [count, windowStart]

  const send = (res, status, body) => {
    res.writeHead(status, { "Content-Type": "application/json; charset=utf-8", "Cache-Control": "no-store", "X-Content-Type-Options": "nosniff" });
    res.end(JSON.stringify(body));
  };
  const readJson = req => new Promise((resolve, reject) => {
    let size = 0; const chunks = [];
    req.on("data", c => { size += c.length; if (size > 64 * 1024) { reject(new BadRequest("That request is too big.")); req.destroy(); } else chunks.push(c); });
    req.on("end", () => { try { resolve(chunks.length ? JSON.parse(Buffer.concat(chunks).toString("utf8")) : {}); } catch { reject(new BadRequest("That request isn't valid JSON.")); } });
    req.on("error", reject);
  });

  return async function handle(req, res) {
    const url = new URL(req.url, "http://localhost");
    try {
      if (req.method === "GET" && url.pathname === "/health") return send(res, 200, { ok: true, mode, version: VERSION });
      if (!url.pathname.startsWith("/api/")) return send(res, 404, { error: "Not found." });
      if (env.APP_KEY && req.headers["x-app-key"] !== env.APP_KEY) return send(res, 401, { error: "This app isn't allowed to use the server." });
      // A simple limit per address: 60 requests a minute.
      const ip = req.socket.remoteAddress ?? "";
      const t = Date.now(), [count, start] = hits.get(ip) ?? [0, t];
      const fresh = t - start > 60_000;
      hits.set(ip, fresh ? [1, t] : [count + 1, start]);
      if (!fresh && count >= 60) return send(res, 429, { error: "Too many requests. Wait a minute and try again." });

      if (req.method === "GET" && url.pathname === "/api/places") {
        const q = (url.searchParams.get("q") ?? "").slice(0, 40);
        if (q.trim().length < 2) return send(res, 200, { places: [] });
        return send(res, 200, { places: live ? await live.places(q) : demoPlaces(q) });
      }
      if (req.method === "POST" && url.pathname === "/api/search") {
        const s = checkSearch(await readJson(req), now().toISOString().slice(0, 10));
        const offers = live ? await live.search(s) : demoSearch(s);
        return send(res, 200, { mode, offers });
      }
      const offerMatch = /^\/api\/offers\/([\w-]{3,80})$/.exec(url.pathname);
      if (req.method === "GET" && offerMatch) {
        const offer = live ? await live.offer(offerMatch[1]) : demoOffer(offerMatch[1]);
        return offer ? send(res, 200, { offer }) : send(res, 404, { error: "That fare is no longer available. Search again." });
      }
      if (req.method === "POST" && url.pathname === "/api/bookings") {
        // Real bookings are paid from your Duffel balance. Until the app collects payment from travellers,
        // live bookings stay off unless you turn them on on purpose.
        if (mode === "live" && env.LIVE_BOOKINGS !== "on") return send(res, 403, { error: "Booking isn't open yet. You can search and compare flights in the meantime." });
        const body = await readJson(req);
        const offerId = String(body.offerId ?? "");
        if (!/^[\w-]{3,80}$/.test(offerId)) throw new BadRequest("Choose a flight first.");
        const offer = live ? await live.offer(offerId) : demoOffer(offerId);
        if (!offer) return send(res, 404, { error: "That fare is no longer available. Search again." });
        const passengers = checkPassengers(body.passengers, offer.passengerIds);
        const booking = live ? await live.book(offerId, passengers) : demoBook(offerId, passengers);
        return send(res, 201, { booking });
      }
      const bookingMatch = /^\/api\/bookings\/([\w-]{3,80})$/.exec(url.pathname);
      if (req.method === "GET" && bookingMatch) {
        const booking = live ? await live.booking(bookingMatch[1]) : demoBooking(bookingMatch[1]);
        return booking ? send(res, 200, { booking }) : send(res, 404, { error: "That booking wasn't found." });
      }
      return send(res, 404, { error: "Not found." });
    } catch (e) {
      if (e instanceof BadRequest) return send(res, 400, { error: e.message });
      if (e instanceof DuffelError) return send(res, e.status, { error: e.message });
      console.error(e);
      return send(res, 500, { error: "Something went wrong on the server. Try again." });
    }
  };
}

/** Reads server/.env (KEY=value lines) into process.env, without overriding what's already set. */
export async function loadEnv(file) {
  const fs = await import("node:fs");
  if (!fs.existsSync(file)) return;
  for (const line of fs.readFileSync(file, "utf8").split(/\r?\n/)) {
    const m = /^\s*([A-Z_][A-Z0-9_]*)\s*=\s*(.*?)\s*$/.exec(line);
    if (m && !line.trim().startsWith("#") && process.env[m[1]] === undefined) process.env[m[1]] = m[2].replace(/^["']|["']$/g, "");
  }
}

if (import.meta.url === `file:///${process.argv[1]?.replace(/\\/g, "/").replace(/^\//, "")}` || process.argv[1]?.endsWith("server.js")) {
  const path = await import("node:path");
  await loadEnv(path.join(path.dirname(new URL(import.meta.url).pathname.replace(/^\/([A-Z]:)/, "$1")), "..", ".env"));
  const port = Number(process.env.PORT ?? 8787);
  const handler = createHandler(process.env);
  http.createServer(handler).listen(port, () => console.log(`Veyra Flights server on http://localhost:${port} (${process.env.DUFFEL_TOKEN ? "Duffel" : "demo flights"})`));
}
