document.addEventListener("DOMContentLoaded", function () {

// Cấu hình
const config = {
  edgeThreshold: 20,    // Khoảng cách dính viền
  headerHeight: 80,     // Chiều cao header
  autoShowDelay: 3000,  // Thời gian tự động hiển thị chatbox (ms)
  transitionDuration: 250 // Thời gian transition (ms)
};

// DOM elements
const elements = {
  backToTopBtn: document.getElementById("backToTop"),
  chatbox: document.querySelector(".chatbox"),
  chatToggle: document.getElementById("chatToggle"),
  chatWindow: document.querySelector(".chat-window"),
  closeChat: document.getElementById("closeChat"),
  quickQuestions: document.querySelectorAll(".quick-question"),
  chatInput: document.querySelector(".chat-input input"),
  sendBtn: document.querySelector(".send-btn"),
  chatMessages: document.querySelector(".chat-messages"),
  messageSound: document.getElementById("messageSound"),
};

// Kiểm tra null cho các phần tử DOM
const checkDOMElements = () => {
  for (const [key, value] of Object.entries(elements)) {
    if (!value && key !== "messageSound") {
      console.error(`Element "${key}" not found in DOM`);
      return false;
    }
  }
  return true;
};

// Back to Top
if (elements.backToTopBtn) {
  window.addEventListener("scroll", () => {
    elements.backToTopBtn.style.display = window.scrollY > 300 ? "block" : "none";
  });
  elements.backToTopBtn.addEventListener("click", () => {
    window.scrollTo({ top: 0, behavior: "smooth" });
  });
}

// Chatbox functionality
if (checkDOMElements()) {
  let isDragging = false;
  let offsetX, offsetY;

  // Định vị thông minh: Chỉ dính vào viền trái hoặc phải
  const getSmartPosition = (x, y) => {
    const { innerWidth: w, innerHeight: h } = window;
    const { width, height } = elements.chatbox.getBoundingClientRect();

    // Chỉ kiểm tra viền trái và phải
    const distances = {
      left: x,
      right: w - x - width,
    };

    // Tìm viền gần nhất
    const closestEdge = Object.keys(distances).reduce((a, b) => distances[a] < distances[b] ? a : b);
    let newX = x;
    let newY = y;
    let edge = closestEdge;

    // Đẩy chatbox sát viền trái hoặc phải
    switch (closestEdge) {
      case "left":
        newX = config.edgeThreshold;
        break;
      case "right":
        newX = w - width - config.edgeThreshold;
        break;
    }

    // Giới hạn trong viewport
    newX = Math.max(0, Math.min(newX, w - width));
    newY = Math.max(config.headerHeight, Math.min(newY, h - height));

    console.log(`Snapping to ${edge}: newX=${newX}, newY=${newY}`);
    return { x: newX, y: newY, edge };
  };

  // Kiểm tra và tự động dính vào viền
  const checkAndSnapToEdge = () => {
    const rect = elements.chatbox.getBoundingClientRect();
    elements.chatbox.style.transition = `left ${config.transitionDuration}ms cubic-bezier(0.4, 0, 0.2, 1), top ${config.transitionDuration}ms cubic-bezier(0.4, 0, 0.2, 1)`;
    const { x, y, edge } = getSmartPosition(rect.left, rect.top);
    elements.chatbox.style.left = `${x}px`;
    elements.chatbox.style.top = `${y}px`;
    elements.chatbox.style.right = "auto";
    elements.chatbox.style.bottom = "auto";
    elements.chatbox.classList.add("auto-positioning");

    // Lưu vị trí
    localStorage.setItem("chatboxPosition", JSON.stringify({ left: x, top: y, edge }));
    determineChatWindowPosition(x, y, edge);

    // Xóa transition sau khi hoàn tất
    setTimeout(() => {
      elements.chatbox.classList.remove("auto-positioning");
      elements.chatbox.style.transition = "";
    }, config.transitionDuration);
  };

  // Khôi phục vị trí chatbox
  const restoreChatboxPosition = () => {
    const savedPos = localStorage.getItem("chatboxPosition");
    if (savedPos && elements.chatbox) {
      const pos = JSON.parse(savedPos);
      elements.chatbox.style.left = `${pos.left}px`;
      elements.chatbox.style.top = `${pos.top}px`;
      elements.chatbox.style.right = "auto";
      elements.chatbox.style.bottom = "auto";
      determineChatWindowPosition(pos.left, pos.top, pos.edge);
      checkAndSnapToEdge();
    }
  };

  // Xác định hướng mở bảng chat với Opposite-Side Expansion
  const determineChatWindowPosition = (x, y, edge) => {
    const { innerWidth: w, innerHeight: h } = window;
    const chatWindowWidth = 320; // Chiều rộng của .chat-window
    const chatboxWidth = elements.chatbox.getBoundingClientRect().width;

    elements.chatWindow.classList.remove("right-side", "left-side");

    // Hàm kiểm tra không gian khả dụng
    const hasEnoughSpace = (direction) => {
      switch (direction) {
        case "right":
          return x + chatboxWidth + chatWindowWidth + config.edgeThreshold <= w;
        case "left":
          return x - chatWindowWidth - config.edgeThreshold >= 0;
        default:
          return false;
      }
    };

    // Ưu tiên Opposite-Side Expansion
    let selectedSide = null;
    switch (edge) {
      case "left":
        if (hasEnoughSpace("right")) {
          selectedSide = "right-side";
          console.log(`Chat window: right-side (opposite of left edge, enough space on right: ${w - (x + chatboxWidth)}px)`);
        } else {
          selectedSide = "left-side";
          console.log(`Chat window: left-side (opposite of left edge, not enough space on right, fallback to left)`);
        }
        break;
      case "right":
        if (hasEnoughSpace("left")) {
          selectedSide = "left-side";
          console.log(`Chat window: left-side (opposite of right edge, enough space on left: ${x}px)`);
        } else {
          selectedSide = "right-side";
          console.log(`Chat window: right-side (opposite of right edge, not enough space on left, fallback to right)`);
        }
        break;
      default:
        // Trường hợp chatbox không ở viền trái/phải
        if (x < w / 2 && hasEnoughSpace("right")) {
          selectedSide = "right-side";
          console.log(`Chat window: right-side (chatbox in left half, enough space on right: ${w - (x + chatboxWidth)}px)`);
        } else if (x >= w / 2 && hasEnoughSpace("left")) {
          selectedSide = "left-side";
          console.log(`Chat window: left-side (chatbox in right half, enough space on left: ${x}px)`);
        } else {
          selectedSide = "right-side";
          console.log(`Chat window: right-side (default, fallback to right)`);
        }
        break;
    }

    // Áp dụng class và điều chỉnh vị trí động nếu cần
    elements.chatWindow.classList.add(selectedSide);

    // Đảm bảo bảng chat nằm trong viewport
    const chatWindowRect = elements.chatWindow.getBoundingClientRect();
    if (selectedSide === "right-side" && chatWindowRect.right > w) {
      elements.chatWindow.style.left = `${w - chatWindowWidth - config.edgeThreshold}px`;
      elements.chatWindow.style.right = "auto";
      console.log(`Adjusted chat window left to ${w - chatWindowWidth - config.edgeThreshold}px to fit viewport`);
    } else if (selectedSide === "left-side" && chatWindowRect.left < 0) {
      elements.chatWindow.style.right = `${w - x - chatboxWidth - config.edgeThreshold}px`;
      elements.chatWindow.style.left = "auto";
      console.log(`Adjusted chat window right to ${w - x - chatboxWidth - config.edgeThreshold}px to fit viewport`);
    }
  };

  // Mở chatbox tự động
  setTimeout(() => {
    if (elements.chatWindow) {
      elements.chatWindow.classList.add("active");
      if (elements.messageSound) {
        elements.messageSound.play().catch(() => console.warn("Failed to play sound"));
      }
      checkAndSnapToEdge();
    }
  }, config.autoShowDelay);

  // Toggle chatbox
  elements.chatToggle?.addEventListener("click", (e) => {
    e.preventDefault();
    e.stopPropagation();
    if (elements.chatWindow) {
      const isHidden = !elements.chatWindow.classList.contains("active");
      elements.chatWindow.classList.toggle("active");
      console.log(`Chat window toggled: ${isHidden ? "shown" : "hidden"}`);
      if (isHidden) {
        checkAndSnapToEdge(); // Cập nhật vị trí bảng chat khi mở
      }
    } else {
      console.error("Chat window element not found");
    }
  });

  elements.closeChat?.addEventListener("click", (e) => {
    e.stopPropagation();
    if (elements.chatWindow) {
      elements.chatWindow.classList.remove("active");
      console.log("Chat window closed");
    }
  });

  // Gửi tin nhắn
  const sendMessage = () => {
    const message = elements.chatInput.value.trim();
    if (message) {
      addMessage(message, "user");
      elements.chatInput.value = "";
      setTimeout(() => {
        addMessage("Cảm ơn bạn đã liên hệ! Chúng tôi sẽ phản hồi sớm nhất.", "bot");
        elements.messageSound?.play().catch(() => console.warn("Failed to play sound"));
      }, 1000);
    }
  };

  elements.sendBtn?.addEventListener("click", sendMessage);
  elements.chatInput?.addEventListener("keypress", (e) => {
    if (e.key === "Enter") sendMessage();
  });

  // Câu hỏi nhanh
  elements.quickQuestions.forEach((question) => {
    question.addEventListener("click", () => {
      addMessage(question.textContent, "user");
      setTimeout(() => {
        addMessage("Chúng tôi đã nhận được yêu cầu của bạn...", "bot");
        elements.messageSound?.play().catch(() => console.warn("Failed to play sound"));
      }, 800);
    });
  });

  // Thêm tin nhắn
  const addMessage = (text, sender) => {
    const messageDiv = document.createElement("div");
    messageDiv.classList.add("message", sender);
    messageDiv.textContent = text;
    elements.chatMessages.appendChild(messageDiv);
    elements.chatMessages.scrollTop = elements.chatMessages.scrollHeight;
  };

  // Drag and Drop
  const startDrag = (e) => {
    const header = e.target.closest(".chat-header");
    const toggleBtn = e.target.closest("#chatToggle");
    if (!header && !toggleBtn) return;

    isDragging = true;
    elements.chatbox.classList.add("dragging");
    elements.chatbox.style.transition = "";

    const rect = elements.chatbox.getBoundingClientRect();
    if (e.type === "mousedown") {
      offsetX = e.clientX - rect.left;
      offsetY = e.clientY - rect.top;
    } else {
      e.preventDefault();
      offsetX = e.touches[0].clientX - rect.left;
      offsetY = e.touches[0].clientY - rect.top;
    }
  };

  const drag = (e) => {
    if (!isDragging) return;
    e.preventDefault();

    let x = e.type === "mousemove" ? e.clientX - offsetX : e.touches[0].clientX - offsetX;
    let y = e.type === "mousemove" ? e.clientY - offsetY : e.touches[0].clientY - offsetY;

    elements.chatbox.style.left = `${x}px`;
    elements.chatbox.style.top = `${y}px`;
    elements.chatbox.style.right = "auto";
    elements.chatbox.style.bottom = "auto";
  };

  const endDrag = () => {
    if (!isDragging) return;
    isDragging = false;
    elements.chatbox.classList.remove("dragging");
    checkAndSnapToEdge();
  };

  // Gắn sự kiện
  elements.chatbox.addEventListener("mousedown", startDrag);
  elements.chatbox.addEventListener("touchstart", startDrag, { passive: false });
  document.addEventListener("mousemove", drag);
  document.addEventListener("touchmove", drag, { passive: false });
  document.addEventListener("mouseup", endDrag);
  document.addEventListener("touchend", endDrag);

  // Khởi tạo
  window.addEventListener("load", restoreChatboxPosition);
  window.addEventListener("resize", () => {
    const rect = elements.chatbox.getBoundingClientRect();
    if (rect.right > window.innerWidth || rect.bottom > window.innerHeight) {
      elements.chatbox.style.left = "auto";
      elements.chatbox.style.right = "20px";
      elements.chatbox.style.top = "auto";
      elements.chatbox.style.bottom = "20px";
      localStorage.removeItem("chatboxPosition");
      checkAndSnapToEdge();
    }
  });
}

// Slideshow sản phẩm home
const track = document.getElementById('carouselTrack');
const itemWidth = 250; // Rộng mỗi sản phẩm (px)
const itemCount = 10; // Số sản phẩm gốc
const totalWidth = itemWidth * itemCount; // Độ rộng 10 sản phẩm gốc

function resetScroll() {
  const currentTransform = parseFloat(getComputedStyle(track).transform.split(',')[4]) || 0;
  if (Math.abs(currentTransform) >= totalWidth) {
    track.style.transition = 'none';
    track.style.transform = 'translateX(0)';
    void track.offsetWidth; // Force reflow
    track.style.transition = 'transform 0.5s linear';
  }
  requestAnimationFrame(resetScroll);
}

requestAnimationFrame(resetScroll);

const $window = $(window);
const $body = $('body');

class Slideshow {
    constructor(userOptions = {}) {
        const defaultOptions = {
            $el: $('.slideshow'),
            showArrows: true,
            showPagination: true,
            duration: 4000,
            autoplay: true
        };

        let options = Object.assign({}, defaultOptions, userOptions);

        this.$el = options.$el;
        this.maxSlide = this.$el.find($('.js-slider-home-slide')).length;
        this.showArrows = this.maxSlide > 1 ? options.showArrows : false;
        this.showPagination = options.showPagination;
        this.currentSlide = 1;
        this.isAnimating = false;
        this.animationDuration = 1200;
        this.autoplaySpeed = options.duration;
        this.interval;
        this.$controls = this.$el.find('.js-slider-home-button');

        this.autoplay = this.maxSlide > 1 ? options.autoplay : false;

        this.$el.on('click', '.js-slider-home-next', (event) => this.nextSlide());
        this.$el.on('click', '.js-slider-home-prev', (event) => this.prevSlide());
        this.$el.on('click', '.js-pagination-item', event => {
            if (!this.isAnimating) {
                this.preventClick();
                this.goToSlide(event.target.dataset.slide);
            }
        });

        this.init();
    }

    init() {
        this.goToSlide(1);
        if (this.autoplay) {
            this.startAutoplay();
        }

        if (this.showPagination) {
            let paginationNumber = this.maxSlide;
            let pagination = '<div class="pagination"><div class="container">';
            for (let i = 0; i < this.maxSlide; i++) {
                let item = `<span class="pagination__item js-pagination-item ${i === 0 ? 'is-current' : ''}" data-slide="${i + 1}">${i + 1}</span>`;
                pagination = pagination + item;
            }
            pagination = pagination + '</div></div>';
            this.$el.find('.pagination .container').append(pagination);
        }
    }

    preventClick() {
        this.isAnimating = true;
        this.$controls.prop('disabled', true);
        clearInterval(this.interval);
        setTimeout(() => {
            this.isAnimating = false;
            this.$controls.prop('disabled', false);
            if (this.autoplay) {
                this.startAutoplay();
            }
        }, this.animationDuration);
    }

    goToSlide(index) {
        this.currentSlide = parseInt(index);
        if (this.currentSlide > this.maxSlide) {
            this.currentSlide = 1;
        }
        if (this.currentSlide === 0) {
            this.currentSlide = this.maxSlide;
        }

        const newCurrent = this.$el.find('.js-slider-home-slide[data-slide="'+ this.currentSlide +'"]');
        const newPrev = this.currentSlide === 1 ? this.$el.find('.js-slider-home-slide').last() : newCurrent.prev('.js-slider-home-slide');
        const newNext = this.currentSlide === this.maxSlide ? this.$el.find('.js-slider-home-slide').first() : newCurrent.next('.js-slider-home-slide');

        this.$el.find('.js-slider-home-slide').removeClass('is-prev is-next is-current');
        this.$el.find('.js-pagination-item').removeClass('is-current');

        if (this.maxSlide > 1) {
            newPrev.addClass('is-prev');
            newNext.addClass('is-next');
        }

        newCurrent.addClass('is-current');
        this.$el.find('.js-pagination-item[data-slide="'+this.currentSlide+'"]').addClass('is-current');
    }

    nextSlide() {
        this.preventClick();
        this.goToSlide(this.currentSlide + 1);
    }

    prevSlide() {
        this.preventClick();
        this.goToSlide(this.currentSlide - 1);
    }

    startAutoplay() {
        this.interval = setInterval(() => {
            if (!this.isAnimating) {
                this.nextSlide();
            }
        }, this.autoplaySpeed);
    }

    destroy() {
        this.$el.off();
    }
}

(function() {
    let loaded = false;
    let maxLoad = 3000;

    function load() {
        const options = {
            showPagination: true,
            showArrows: true
        };
        let slideShow = new Slideshow(options);
    }

    function addLoadClass() {
        $body.addClass('is-loaded');
        setTimeout(function() {
            $body.addClass('is-animated');
        }, 600);
    }

    $window.on('load', function() {
        if (!loaded) {
            loaded = true;
            load();
        }
    });

    setTimeout(function() {
        if (!loaded) {
            loaded = true;
            load();
        }
    }, maxLoad);

    addLoadClass();
})();

});