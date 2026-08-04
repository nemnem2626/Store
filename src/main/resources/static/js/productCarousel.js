export function initProductCarousel(trackId = 'carouselTrack', itemWidth = 250, itemCount = 10) {
  const track = document.getElementById(trackId);
  if (!track) return;
  const totalWidth = itemWidth * itemCount;

  function resetScroll() {
    const currentTransform = parseFloat(getComputedStyle(track).transform.split(',')[5]) || 0;
    if (Math.abs(currentTransform) >= totalWidth) {
      track.style.transition = 'none';
      track.style.transform = 'translateX(0)';
      void track.offsetWidth;
      track.style.transition = 'transform 0.5s linear';
    }
    requestAnimationFrame(resetScroll);
  }

  requestAnimationFrame(resetScroll);
}
