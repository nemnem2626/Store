// Hộp thư hỗ trợ phía nhân viên: chọn khách hàng, xem hội thoại và trả lời.
(function () {
  const list = document.getElementById("scInboxList");
  const thread = document.getElementById("scThread");
  const title = document.getElementById("scThreadTitle");
  const form = document.getElementById("scReplyForm");
  const input = document.getElementById("scReplyInput");
  const button = document.getElementById("scReplyBtn");
  if (!list || !thread) return;

  const POLL_MS = 4000;
  let currentId = null;
  let lastId = 0;

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
    messages.forEach(function (message) {
      const item = document.createElement("div");
      item.className =
        "sc-msg " + (message.senderRole === "STAFF" ? "user" : "staff");
      item.textContent = message.content;
      const time = document.createElement("span");
      time.className = "sc-msg-time";
      time.textContent = formatTime(message.createdAt);
      item.appendChild(time);
      thread.appendChild(item);
      if (message.id > lastId) lastId = message.id;
    });
    if (messages.length) thread.scrollTop = thread.scrollHeight;
  }

  function poll() {
    if (!currentId) return;
    fetch(BASE_PATH + "/" + currentId + "/messages?afterId=" + lastId, {
      credentials: "same-origin",
    })
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

  list.addEventListener("click", function (event) {
    const item = event.target.closest(".sc-inbox-item");
    if (!item) return;
    list.querySelectorAll(".sc-inbox-item").forEach(function (el) {
      el.classList.remove("active");
    });
    item.classList.add("active");
    const badge = item.querySelector(".ad-badge");
    if (badge) badge.remove();
    currentId = item.getAttribute("data-id");
    lastId = 0;
    thread.innerHTML = "";
    title.textContent = "Hội thoại với " + item.getAttribute("data-name");
    input.disabled = false;
    button.disabled = false;
    poll();
  });

  form.addEventListener("submit", function (event) {
    event.preventDefault();
    const content = input.value.trim();
    if (!content || !currentId) return;
    input.value = "";
    fetch(BASE_PATH + "/" + currentId + "/send", {
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

  setInterval(poll, POLL_MS);
})();
