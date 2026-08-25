// Hộp chat hỗ trợ phía khách hàng: gửi tin cho nhân viên và tự động nhận trả lời.
(function () {
  const panel = document.getElementById("scPanel");
  if (!panel) return;

  const launcher = document.getElementById("scLauncher");
  const badge = document.getElementById("scBadge");
  const body = document.getElementById("scBody");
  const form = document.getElementById("scForm");
  const input = document.getElementById("scInput");
  const POLL_MS = 4000;

  let lastId = 0;
  let unread = 0;
  let timer = null;

  function csrfParam() {
    const field = form.querySelector(".sc-csrf");
    if (!field || !field.name) return "";
    return "&" + encodeURIComponent(field.name) + "=" + encodeURIComponent(field.value);
  }

  function formatTime(ms) {
    if (!ms) return "";
    return new Date(ms).toLocaleString("vi-VN", {
      hour: "2-digit",
      minute: "2-digit",
      day: "2-digit",
      month: "2-digit",
    });
  }

  function render(messages) {
    const empty = body.querySelector(".sc-empty");
    if (messages.length && empty) empty.remove();
    messages.forEach(function (message) {
      const item = document.createElement("div");
      item.className =
        "sc-msg " + (message.senderRole === "STAFF" ? "staff" : "user");
      item.textContent = message.content;
      const time = document.createElement("span");
      time.className = "sc-msg-time";
      time.textContent = formatTime(message.createdAt);
      item.appendChild(time);
      body.appendChild(item);
      if (message.id > lastId) lastId = message.id;
      if (message.senderRole === "STAFF" && !panel.classList.contains("open")) {
        unread += 1;
      }
    });
    if (messages.length) body.scrollTop = body.scrollHeight;
    badge.style.display = unread > 0 ? "block" : "none";
    badge.textContent = unread > 9 ? "9+" : String(unread);
  }

  function poll() {
    fetch("/chat/messages?afterId=" + lastId, { credentials: "same-origin" })
      .then(function (res) {
        return res.ok ? res.json() : null;
      })
      .then(function (data) {
        if (data && data.messages) render(data.messages);
      })
      .catch(function () {
        /* bỏ qua lỗi mạng tạm thời */
      });
  }

  launcher.addEventListener("click", function () {
    panel.classList.toggle("open");
    if (panel.classList.contains("open")) {
      unread = 0;
      badge.style.display = "none";
      input.focus();
      body.scrollTop = body.scrollHeight;
    }
  });

  document.getElementById("scClose").addEventListener("click", function () {
    panel.classList.remove("open");
  });

  form.addEventListener("submit", function (event) {
    event.preventDefault();
    const content = input.value.trim();
    if (!content) return;
    input.value = "";
    fetch("/chat/send", {
      method: "POST",
      credentials: "same-origin",
      headers: { "Content-Type": "application/x-www-form-urlencoded" },
      body: "content=" + encodeURIComponent(content) + csrfParam(),
    })
      .then(function (res) {
        return res.json();
      })
      .then(function (data) {
        if (data && data.message) render([data.message]);
      })
      .catch(function () {
        input.value = content;
      });
  });

  poll();
  timer = setInterval(poll, POLL_MS);
  window.addEventListener("beforeunload", function () {
    clearInterval(timer);
  });
})();
