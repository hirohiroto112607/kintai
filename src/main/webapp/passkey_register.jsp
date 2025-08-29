<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<!DOCTYPE html>
<html lang="ja">
<head>
  <meta charset="UTF-8" />
  <meta name="viewport" content="width=device-width,initial-scale=1" />
  <title>パスキー登録 - 勤怠管理システム</title>
  <link rel="stylesheet" href="style.css" />
</head>
<body>
  <div class="container">
    <h1>パスキー登録</h1>
    <p>このページはログイン済みユーザー向けのパスキー登録ページです。先に通常ログインしてください。</p>

    <div id="status" class="message"></div>
    <button id="start-register">パスキーを作成する</button>

    <p>
      作成後は自動でサーバに登録します。問題があればコンソールとサーバログを確認してください。
    </p>
  </div>

  <script>
    // base64url helper
    const base64url = {
      encode: (buffer) => {
        return btoa(String.fromCharCode(...new Uint8Array(buffer)))
          .replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
      },
      decode: (str) => {
        str = str.replace(/-/g, '+').replace(/_/g, '/');
        while (str.length % 4) str += '=';
        const binary = atob(str);
        const buf = new Uint8Array(binary.length);
        for (let i = 0; i < binary.length; i++) buf[i] = binary.charCodeAt(i);
        return buf.buffer;
      }
    };

    const statusEl = document.getElementById('status');
    const btn = document.getElementById('start-register');

    function setStatus(text, ok = true) {
      statusEl.textContent = text;
      statusEl.className = ok ? 'success-message' : 'error-message';
    }

    btn.addEventListener('click', async () => {
      try {
        setStatus('開始しています...');

        // 1) サーバから作成オプションを取得
        const startRes = await fetch('passkey/register/start', { credentials: 'same-origin' });
        if (!startRes.ok) {
          const txt = await startRes.text();
          throw new Error('作成開始に失敗: ' + txt);
        }
        const options = await startRes.json();

        // 2) WebAuthn 用に整形（challenge, user.id, excludeCredentials の id を ArrayBuffer に）
        const publicKey = {
          challenge: base64url.decode(options.challenge),
          rp: options.rp, // optional, ignored by navigator
          user: options.user ? {
            id: base64url.decode(options.user.id),
            name: options.user.name,
            displayName: options.user.displayName
          } : undefined,
          pubKeyCredParams: options.pubKeyCredParams || options.publicKeyCredParams || undefined,
          timeout: typeof options.timeout === 'number' ? options.timeout : undefined,
          attestation: options.attestation || undefined,
          authenticatorSelection: options.authenticatorSelection || undefined
        };

        if (Array.isArray(options.excludeCredentials)) {
          publicKey.excludeCredentials = options.excludeCredentials.map(c => ({
            type: c.type || 'public-key',
            id: base64url.decode(c.id),
            transports: Array.isArray(c.transports) ? c.transports : undefined
          }));
        }

        // 3) navigator.credentials.create を呼ぶ
        const credential = await navigator.credentials.create({ publicKey });
        if (!credential) throw new Error('Credential 作成が null を返しました');

         // 4) サーバへ送信（Finish サーブレットで期待する最小フォーマット）
        const body = {
          id: credential.id,
          rawId: base64url.encode(credential.rawId),
          response: {
            clientDataJSON: base64url.encode(credential.response.clientDataJSON),
            attestationObject: base64url.encode(credential.response.attestationObject)
          }
        };

        const finishRes = await fetch('passkey/register/finish', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'same-origin',
          body: JSON.stringify(body)
        });

        if (!finishRes.ok) {
          const txt = await finishRes.text();
          throw new Error('登録完了でエラー: ' + txt);
        }

        setStatus('パスキーを登録しました。', true);
      } catch (err) {
        console.error('登録エラー', err);
        setStatus('エラー: ' + (err.message || err), false);
      }
    });
  </script>
</body>
</html>
