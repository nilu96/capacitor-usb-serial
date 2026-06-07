import { registerPlugin } from '@capacitor/core';

import type { UsbSerialPlugin } from './definitions';

const UsbSerial = registerPlugin<UsbSerialPlugin>('UsbSerial', {
  web: () => import('./web').then(m => new m.WebUsbSerial()),
});

export * from './definitions';
export { UsbSerial };
