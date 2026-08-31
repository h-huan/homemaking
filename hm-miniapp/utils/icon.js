const ICON_MAP = {
  clean: '/assets/menu-icons/clean.svg',
  'deep-clean': '/assets/menu-icons/deep-clean.svg',
  kitchen: '/assets/menu-icons/kitchen.svg',
  repair: '/assets/menu-icons/repair.svg',
  childcare: '/assets/menu-icons/childcare.svg',
  pet: '/assets/menu-icons/pet.svg',
  order: '/assets/menu-icons/order.svg',
  schedule: '/assets/menu-icons/schedule.svg',
  address: '/assets/menu-icons/address.svg',
  contact: '/assets/menu-icons/contact.svg',
  privacy: '/assets/menu-icons/privacy.svg',
  guide: '/assets/menu-icons/guide.svg',
  about: '/assets/menu-icons/about.svg',
  default: '/assets/menu-icons/default.svg'
}

function isAssetPath(value) {
  return typeof value === 'string'
    && (value.indexOf('http://') === 0
      || value.indexOf('https://') === 0
      || value.indexOf('/assets/') === 0
      || value.indexOf('data:image') === 0)
}

function resolveIcon(value) {
  if (!value) {
    return ICON_MAP.default
  }
  if (isAssetPath(value)) {
    return value
  }
  return ICON_MAP[value] || ICON_MAP.default
}

module.exports = {
  ICON_MAP,
  resolveIcon
}
