import { initBackToTop } from './backtotop.js';
/*import { initHeaderEffects } from './header.js';*/
import { initProductCarousel } from './productCarousel.js';
import { initBannerSlideshow } from './homepageSlideshow.js';
import { initChatbox } from './chatbox.js';
import { initProductSort } from './product.js';
import { initCheckout } from './checkout.js';
import { initLogin } from './login.js';

document.addEventListener("DOMContentLoaded", function () {
  initBackToTop();
/*  initHeaderEffects();*/
  initProductCarousel();
  initBannerSlideshow();
  initChatbox();
  initProductSort();
  initCheckout();
  initLogin();
});
