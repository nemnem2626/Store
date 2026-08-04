export function initLogin() {
  document.addEventListener("DOMContentLoaded", () => {
    const togglePassword = document.querySelector("#togglePassword");
    const passwordInput = document.querySelector("#password");

    if (!togglePassword || !passwordInput) {
      console.warn("Không tìm thấy phần tử toggle hoặc password input.");
      return;
    }

    togglePassword.addEventListener("click", () => {
      const type = passwordInput.getAttribute("type") === "password" ? "text" : "password";
      passwordInput.setAttribute("type", type);
      togglePassword.classList.toggle("visible");
    });
  });
}
