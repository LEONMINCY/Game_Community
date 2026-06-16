// 文件说明：utils/tools.js，媒体地址和图片兜底工具，统一处理头像、图片、视频的访问路径。
const staticMarkers = ['/noLogin/common/img/', '/noLogin/common/thumb/', '/files/', '/uploads/']
const fallbackSvg = encodeURIComponent(`
<svg xmlns="http://www.w3.org/2000/svg" width="320" height="180" viewBox="0 0 320 180">
  <rect width="320" height="180" rx="10" fill="#f3f6fa"/>
  <rect x="112" y="48" width="96" height="66" rx="12" fill="#dce3ec"/>
  <circle cx="137" cy="72" r="10" fill="#aab6c6"/>
  <path d="M122 106l34-28 22 20 12-12 22 20v18h-90z" fill="#aab6c6"/>
</svg>`)
const fallbackImage = `data:image/svg+xml;charset=UTF-8,${fallbackSvg}`

//替换地址
export const replaceURL = (url) => {
  if (!url) {
    return ''
  }
  const value = String(url).trim().replace(/\\/g, '/')
  if (/^[a-zA-Z]:\//.test(value)) {
    const fileName = value.slice(value.lastIndexOf('/') + 1)
    return fileName ? `/noLogin/common/img/${encodeURIComponent(fileName)}` : ''
  }
  const marker = staticMarkers.find(item => value.includes(item))
  if (!marker) {
    return value
  }
  return value.slice(value.indexOf(marker))
}

export const thumbnailURL = (url, width = 420) => {
  if (!url) {
    return ''
  }
  const value = replaceURL(url)
  const marker = '/noLogin/common/img/'
  const markerIndex = value.indexOf(marker)
  if (markerIndex === -1) {
    return value
  }
  const prefix = value.slice(0, markerIndex)
  const rawName = value.slice(markerIndex + marker.length).split(/[?#]/)[0]
  if (!rawName) {
    return value
  }
  return `${prefix}/noLogin/common/thumb/${encodeURIComponent(rawName)}?w=${width}`
}

export const imageFallbackURL = () => fallbackImage

export const handleImageError = (event) => {
  const target = event?.target
  if (!target || target.dataset.fallbackApplied === '1') {
    return
  }
  target.dataset.fallbackApplied = '1'
  target.src = fallbackImage
}

//去标签
export const removeTags = (str) => {
  return str.replace(/<[^>]*>/g, '');
}


