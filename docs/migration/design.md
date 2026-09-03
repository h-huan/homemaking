# HM SaaS architecture

Modular Spring Boot monolith, Java 17 and Vue 3 / TypeScript / Element Plus. Exact upstream revisions are recorded in THIRD-PARTY-NOTICES.md.

Default modules: system, infra, pay, mp, homemaking. CRM, ERP, Mall, BPM, AI, MES, WMS, HRM, FMS, IM, member, report and IoT source remains available but excluded from the application dependency graph.

Customer identity belongs to the platform. Tenant customer relations carry the tenant's local relationship; all operations on tenant business data require a valid tenant context. Directly operated headquarters is the default tenant; franchise tenants use the same boundary. Store, Worker, Service, Booking, Order, Review, Aftersale and Settlement are owned by the homemaking module. Payment amounts and transitions are validated server-side, with idempotent callbacks.

Tenant branding contains public website, logo, favicon, palette, login page, home modules, verified domains, miniapp and public-account presentation. Credentials remain private configuration. WeChat identities are keyed by app id + openid, and verified unionid is scoped to the corresponding open platform; identifiers supplied directly by a client cannot establish identity.

Notification decisions intersect platform caps, tenant rules and customer consent. Deduplication, daily and event limits, quiet hours, message level and channel fallback apply before delivery. Persistent outbox delivery must survive retry; SMS is a controlled fallback, not a parallel blast.

Scheduling checks worker skills, areas, shifts, leave and occupied slots. Orders retain price snapshots. Published portal content is separate from drafts; private evidence is stored outside website roots and read only through authorized order endpoints. Settlement records support allocation, refund reversals and audited manual payout records; automatic bank transfers are not implemented.

Admin operations require both system permissions and a bounded homemaking role template. Store-scoped roles filter detail access, writes, lists, totals and financial reports; workers remain restricted to their own tasks. Platform authority requires a headquarters platform role, not merely tenant id 1.

Tenant payment mode is OFFLINE by default; ONLINE and BOTH preserve the existing pay integration. Missing merchant configuration falls back to offline operation. Positive, server-priced orders become paid only after an authorized, audited receipt or a verified online payment result. Append-only receipt/refund/reversal entries and order/settlement balance changes share one transaction. Historical payment callbacks remain valid after a mode change; manual offline receipts cannot overlap an initiated online payment.

订单变更通过 hm_order_change 保留原约定和新约定。未付订单直接应用；已线下支付订单先处理差额，再在同一事务中记账并应用变更。收支凭证及退差额申请关联变更单，pending_change_id 阻止未结清时履约，幂等请求、订单锁与确认报价共同防止重复记账和并发错价。
