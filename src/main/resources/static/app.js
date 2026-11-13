// Login page logic, including the Fingerprint JavaScript agent.
//
// TODO (kick-off call): confirm the v4 JavaScript agent snippet with
// Fingerprint's tech team. Current docs say to import the agent from the
// CDN as https://fpjscdn.net/v4/<PUBLIC_API_KEY>, then call Fingerprint.start()
// and fp.get(), which returns { event_id, visitor_id }.
// Docs: https://docs.fingerprint.com/docs/install-the-javascript-agent
//
// If no public key is configured (the default for this starter), the page
// sends a placeholder request id and the backend stub simulates the signals,
// so the login still works end to end.

let requestId = null;

const statusEl = document.getElementById('fp-status');
const messageEl = document.getElementById('message');
const protectedEl = document.getElementById('protected');

function placeholderId() {
    return 'stub-' + Math.random().toString(36).slice(2);
}

async function identify() {
    try {
        const config = await (await fetch('/api/config')).json();
        if (!config.fingerprintConfigured) {
            requestId = placeholderId();
            statusEl.textContent = 'Fingerprint: stub mode, no public key configured. Using a placeholder id.';
            return;
        }
        const Fingerprint = await import(`https://fpjscdn.net/v4/${config.publicKey}`);
        const fp = await Fingerprint.start();
        const result = await fp.get();
        // v4 calls it event_id, the v3 agent called it requestId.
        requestId = result.event_id || result.requestId;
        statusEl.textContent = 'Fingerprint: device identified.';
    } catch (e) {
        // Never let Fingerprint break the login page: fall back to a
        // placeholder id and let the backend decide what to do with it.
        requestId = placeholderId();
        statusEl.textContent = 'Fingerprint: agent did not load, using a placeholder id.';
        console.warn('Fingerprint agent failed, using placeholder id', e);
    }
}

document.getElementById('login-form').addEventListener('submit', async (event) => {
    event.preventDefault();
    messageEl.textContent = '';
    protectedEl.hidden = true;

    const res = await fetch('/api/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
            username: document.getElementById('username').value,
            password: document.getElementById('password').value,
            requestId: requestId
        })
    });
    const body = await res.json();
    if (!res.ok) {
        messageEl.textContent = body.error || ('Login failed (' + res.status + ')');
        return;
    }
    document.getElementById('who').textContent = body.displayName + ' (' + body.username + ')';
    document.getElementById('fp-detail').textContent =
        'Device: ' + body.visitorId + '. ' + body.fingerprint
        + (body.stepUp ? ' Extra verification required: ' + body.stepUpReason : '');
    protectedEl.hidden = false;
    messageEl.textContent = 'Logged in.';
});

document.getElementById('logout').addEventListener('click', async () => {
    await fetch('/api/logout', { method: 'POST' });
    protectedEl.hidden = true;
    messageEl.textContent = 'Logged out.';
});

identify();
