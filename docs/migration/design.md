# HM SaaS architecture

Modular Spring Boot monolith, Java 17 and Vue 3 / TypeScript / Element Plus. Exact upstream revisions are recorded in THIRD-PARTY-NOTICES.md.

Default modules: system, infra, pay, mp, homemaking. CRM, ERP, Mall, BPM, AI, MES, WMS, HRM, FMS, IM, member, report and IoT source remains available but excluded from the application dependency graph.

Customer identity belongs to the platform. Tenant customer relations carry the tenant's local relationship; all operations on tenant business data require a valid tenant context. Directly operated headquarters is the default tenant; franchise tenants use the same boundary. Store, Worker, Service, Booking, Order, Review, Aftersale and Settlement are owned by the homemaking module. Payment amounts and transitions are validated server-side, with idempotent callbacks.

Tenant branding contains public website, logo, favicon, palette, login page, home modules, verified domains, miniapp and public-account presentation. Credentials remain private configuration. WeChat identities are keyed by app id + openid, and verified unionid is scoped to the corresponding open platform; identifiers supplied directly by a client cannot establish identity.

Notification decisions intersect platform caps, tenant rules and customer consent. Deduplication, daily and event limits, quiet hours, message level and channel fallback apply before delivery. Persistent outbox delivery must survive retry; SMS is a controlled fallback, not a parallel blast.

Scheduling checks worker skills, areas, shifts, leave and occupied slots. Orders retain price snapshots. Published portal content is separate from drafts; private evidence is stored outside website roots and read only through authorized order endpoints. Settlement records support allocation, refund reversals and audited manual payout records; automatic bank transfers are not implemented.
