export function initBackToTop(buttonId = "backToTop") {
  const button = document.getElementById(buttonId);
  if (!button) return;

  window.addEventListener("scroll", () => {
    button.style.display = window.scrollY > 300 ? "block" : "none";
  });

  button.addEventListener("click", () => {
    window.scrollTo({ top: 0, behavior: "smooth" });
  });
}
