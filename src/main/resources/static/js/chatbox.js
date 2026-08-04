// chatbox.js
export function initChatbox() {
  const config = {
    edgeThreshold: 20,
    headerHeight: 0,
    transitionDuration: 300,
    autoShowDelay: 1000
  };

  const elements = {
    chatbox: document.querySelector(".chatbox"),
    chatWindow: document.querySelector(".chat-window"),
    chatToggle: document.getElementById("chatToggle"),
    closeChat: document.getElementById("closeChat"),
    chatInput: document.querySelector(".chat-input input"),
    sendBtn: document.querySelector(".send-btn"),
    quickQuestions: document.querySelectorAll(".quick-question"),
    chatMessages: document.querySelector(".chat-messages"),
    messageSound: document.getElementById("messageSound")
  };

  function checkDOMElements() {
    return Object.values(elements).every(el => el != null);
  }

  if (!checkDOMElements()) {
    console.warn("Một hoặc nhiều phần tử chatbox chưa sẵn sàng.");
    return;
  }

  let isDragging = false;
  let offsetX, offsetY;

  function getSmartPosition(x, y) {
    const { innerWidth: w, innerHeight: h } = window;
    const { width, height } = elements.chatbox.getBoundingClientRect();

    const distances = {
      left: x,
      right: w - x - width
    };

    const closestEdge = distances.left < distances.right ? "left" : "right";
    let newX = closestEdge === "left" ? config.edgeThreshold : w - width - config.edgeThreshold;
    let newY = Math.max(config.headerHeight, Math.min(y, h - height));

    return { x: newX, y: newY, edge: closestEdge };
  }

  function checkAndSnapToEdge() {
    const rect = elements.chatbox.getBoundingClientRect();
    const { x, y, edge } = getSmartPosition(rect.left, rect.top);

    elements.chatbox.style.transition = `left ${config.transitionDuration}ms, top ${config.transitionDuration}ms`;
    elements.chatbox.style.left = `${x}px`;
    elements.chatbox.style.top = `${y}px`;
    elements.chatbox.style.right = "auto";
    elements.chatbox.style.bottom = "auto";
    elements.chatbox.classList.add("auto-positioning");

    localStorage.setItem("chatboxPosition", JSON.stringify({ left: x, top: y, edge }));
    determineChatWindowPosition(x, y, edge);

    setTimeout(() => {
      elements.chatbox.classList.remove("auto-positioning");
      elements.chatbox.style.transition = "";
    }, config.transitionDuration);
  }

  function restoreChatboxPosition() {
    const saved = localStorage.getItem("chatboxPosition");
    if (saved) {
      const { left, top, edge } = JSON.parse(saved);
      elements.chatbox.style.left = `${left}px`;
      elements.chatbox.style.top = `${top}px`;
      elements.chatbox.style.right = "auto";
      elements.chatbox.style.bottom = "auto";
      determineChatWindowPosition(left, top, edge);
      checkAndSnapToEdge();
    }
  }

  function determineChatWindowPosition(x, y, edge) {
    const w = window.innerWidth;
    const chatWindowWidth = 320;
    const chatboxWidth = elements.chatbox.getBoundingClientRect().width;

    elements.chatWindow.classList.remove("right-side", "left-side");

    const hasSpace = dir => {
      if (dir === "right") return x + chatboxWidth + chatWindowWidth + config.edgeThreshold <= w;
      if (dir === "left") return x - chatWindowWidth - config.edgeThreshold >= 0;
      return false;
    };

    let side = "right-side";
    if (edge === "left" && hasSpace("right")) side = "right-side";
    else if (edge === "right" && hasSpace("left")) side = "left-side";
    else if (x < w / 2 && hasSpace("right")) side = "right-side";
    else if (x >= w / 2 && hasSpace("left")) side = "left-side";

    elements.chatWindow.classList.add(side);
  }

  function sendMessage() {
    const msg = elements.chatInput.value.trim();
    if (!msg) return;
    addMessage(msg, "user");
    elements.chatInput.value = "";
    setTimeout(() => {
      addMessage("Cảm ơn bạn đã liên hệ! Chúng tôi sẽ phản hồi sớm nhất.", "bot");
      elements.messageSound?.play().catch(() => {});
    }, 1000);
  }

  function addMessage(text, sender) {
    const div = document.createElement("div");
    div.className = `message ${sender}`;
    div.textContent = text;
    elements.chatMessages.appendChild(div);
    elements.chatMessages.scrollTop = elements.chatMessages.scrollHeight;
  }

  function startDrag(e) {
    const header = e.target.closest(".chat-header");
    const toggle = e.target.closest("#chatToggle");
    if (!header && !toggle) return;

    isDragging = true;
    elements.chatbox.classList.add("dragging");
    const rect = elements.chatbox.getBoundingClientRect();

    if (e.type === "mousedown") {
      offsetX = e.clientX - rect.left;
      offsetY = e.clientY - rect.top;
    } else {
      offsetX = e.touches[0].clientX - rect.left;
      offsetY = e.touches[0].clientY - rect.top;
    }
  }

  function drag(e) {
    if (!isDragging) return;
    e.preventDefault();
    const x = e.type === "mousemove" ? e.clientX : e.touches[0].clientX;
    const y = e.type === "mousemove" ? e.clientY : e.touches[0].clientY;

    elements.chatbox.style.left = `${x - offsetX}px`;
    elements.chatbox.style.top = `${y - offsetY}px`;
    elements.chatbox.style.right = "auto";
    elements.chatbox.style.bottom = "auto";
  }

  function endDrag() {
    if (!isDragging) return;
    isDragging = false;
    elements.chatbox.classList.remove("dragging");
    checkAndSnapToEdge();
  }

  // Gắn sự kiện
  elements.chatToggle.addEventListener("click", (e) => {
    e.preventDefault();
    elements.chatWindow.classList.toggle("active");
    if (elements.chatWindow.classList.contains("active")) {
      checkAndSnapToEdge();
    }
  });

  elements.closeChat.addEventListener("click", () => {
    elements.chatWindow.classList.remove("active");
  });

  elements.sendBtn.addEventListener("click", sendMessage);
  elements.chatInput.addEventListener("keypress", (e) => {
    if (e.key === "Enter") sendMessage();
  });

  elements.quickQuestions.forEach(btn => {
    btn.addEventListener("click", () => {
      addMessage(btn.textContent, "user");
      setTimeout(() => {
        addMessage("Chúng tôi đã nhận được yêu cầu của bạn...", "bot");
        elements.messageSound?.play().catch(() => {});
      }, 800);
    });
  });

  elements.chatbox.addEventListener("mousedown", startDrag);
  elements.chatbox.addEventListener("touchstart", startDrag, { passive: false });
  document.addEventListener("mousemove", drag);
  document.addEventListener("touchmove", drag, { passive: false });
  document.addEventListener("mouseup", endDrag);
  document.addEventListener("touchend", endDrag);

  // Auto show & resize handler
  window.addEventListener("load", () => {
    setTimeout(() => {
      elements.chatWindow.classList.add("active");
      elements.messageSound?.play().catch(() => {});
      checkAndSnapToEdge();
    }, config.autoShowDelay);
    restoreChatboxPosition();
  });

  window.addEventListener("resize", () => {
    const rect = elements.chatbox.getBoundingClientRect();
    const outOfBounds = rect.right > window.innerWidth || rect.bottom > window.innerHeight;
    if (outOfBounds) {
      elements.chatbox.style.left = "auto";
      elements.chatbox.style.right = "20px";
      elements.chatbox.style.top = "auto";
      elements.chatbox.style.bottom = "20px";
      localStorage.removeItem("chatboxPosition");
      checkAndSnapToEdge();
    }
  });
}
