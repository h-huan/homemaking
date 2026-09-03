export interface Link { label: string; href: string }
export interface ContentItem { title: string; text?: string; image?: string }
export interface PortalModule { type: string; title: string; subtitle?: string; enabled: boolean; itemIds?: number[]; items?: ContentItem[] }
export interface PortalConfig { seoTitle: string; seoDescription?: string; announcement?: string; navigation: Link[]; heroTitle: string; heroText?: string; heroImage?: string; primaryAction?: Link; secondaryAction?: Link; modules: PortalModule[]; footerText?: string }
export interface Brand { tenant_name?: string; brand_name?: string; logo?: string; favicon?: string; primary_color?: string; website?: string }
export interface Catalog { services: Record<string, any>[]; stores: Record<string, any>[]; workers: Record<string, any>[]; reviews: Record<string, any>[] }
export interface PortalData { brand: Brand; config: PortalConfig; catalog: Catalog; publishedVersion: number; publishedAt?: string }
