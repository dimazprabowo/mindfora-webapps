export const appUrl = import.meta.env.MINDFORA_URL;

const PROBE_TIMEOUT_MS = 4000;

/** Probe the Laravel health route (/up). `no-cors` = opaque response, but a resolved fetch means the server is reachable. */
export async function isReachable() {
    try {
        await fetch(new URL('/up', appUrl), {
            mode: 'no-cors',
            cache: 'no-store',
            signal: AbortSignal.timeout(PROBE_TIMEOUT_MS),
        });
        return true;
    } catch {
        return false;
    }
}
