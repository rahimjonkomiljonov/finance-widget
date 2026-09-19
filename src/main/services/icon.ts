import { nativeImage, type NativeImage } from 'electron'

// Flat-color placeholder icon (matches the app's blue accent) so the window/tray
// always has an icon even before a real design asset is supplied for packaging.
export function createFallbackIcon(size = 32): NativeImage {
  const buffer = Buffer.alloc(size * size * 4)
  for (let i = 0; i < size * size; i++) {
    buffer[i * 4] = 0xf9 // B
    buffer[i * 4 + 1] = 0x7f // G
    buffer[i * 4 + 2] = 0x2d // R
    buffer[i * 4 + 3] = 0xff // A
  }
  return nativeImage.createFromBitmap(buffer, { width: size, height: size })
}
