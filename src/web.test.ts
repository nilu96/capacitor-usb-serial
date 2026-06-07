import { WebUsbSerial } from './web';

/**
 * Every method on the web stub must reject with code UNSUPPORTED_PLATFORM, and
 * addListener must return a no-op handle that is safe to remove.
 */
describe('WebUsbSerial stub', () => {
  let plugin: WebUsbSerial;

  beforeEach(() => {
    plugin = new WebUsbSerial();
  });

  // method name -> a minimal valid-ish argument object
  const calls: Array<[string, unknown]> = [
    ['listDevices', undefined],
    ['registerDriver', { vendorId: 1, productId: 1, driverType: 'CdcAcmSerialDriver' }],
    ['requestPermission', { deviceId: 'd' }],
    ['hasPermission', { deviceId: 'd' }],
    ['open', { deviceId: 'd' }],
    ['close', { portId: 'p' }],
    ['isOpen', { portId: 'p' }],
    ['getPortInfo', { portId: 'p' }],
    ['setParameters', { portId: 'p', baudRate: 9600, dataBits: 8, stopBits: 1, parity: 'none' }],
    ['read', { portId: 'p' }],
    ['write', { portId: 'p', data: '' }],
    ['writeAsync', { portId: 'p', data: '' }],
    ['startReading', { portId: 'p' }],
    ['stopReading', { portId: 'p' }],
    ['getStreamState', { portId: 'p' }],
    ['getStreamConfig', { portId: 'p' }],
    ['getControlLines', { portId: 'p' }],
    ['getSupportedControlLines', { portId: 'p' }],
    ['getCD', { portId: 'p' }],
    ['getCTS', { portId: 'p' }],
    ['getDSR', { portId: 'p' }],
    ['getDTR', { portId: 'p' }],
    ['getRI', { portId: 'p' }],
    ['getRTS', { portId: 'p' }],
    ['setDTR', { portId: 'p', value: true }],
    ['setRTS', { portId: 'p', value: true }],
    ['setFlowControl', { portId: 'p', mode: 'none' }],
    ['getFlowControl', { portId: 'p' }],
    ['getSupportedFlowControl', { portId: 'p' }],
    ['getXON', { portId: 'p' }],
    ['purgeHwBuffers', { portId: 'p', purgeWrite: true, purgeRead: true }],
    ['setBreak', { portId: 'p', value: true }],
    ['setReadQueue', { portId: 'p', bufferCount: 4, bufferSize: 1024 }],
    ['getReadQueueConfig', { portId: 'p' }],
  ];

  it.each(calls)('%s rejects with UNSUPPORTED_PLATFORM', async (method, arg) => {
    const fn = (plugin as unknown as Record<string, (a: unknown) => Promise<unknown>>)[method];
    expect(typeof fn).toBe('function');
    await expect(fn.call(plugin, arg)).rejects.toMatchObject({ code: 'UNSUPPORTED_PLATFORM' });
  });

  it('addListener returns a no-op handle that can be removed without throwing', async () => {
    const handle = await plugin.addListener('data', () => undefined);
    expect(handle).toBeDefined();
    await expect(handle.remove()).resolves.toBeUndefined();
  });

  it('removeAllListeners resolves', async () => {
    await expect(plugin.removeAllListeners()).resolves.toBeUndefined();
  });
});
