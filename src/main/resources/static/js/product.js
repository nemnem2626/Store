export function initProductSort() {
    // Lọc sản phẩm theo giá
    document.querySelectorAll('.price-sort').forEach(btn => {
        btn.addEventListener('click', function (e) {
            e.preventDefault();
            const sortOrder = this.dataset.sort;

            const container = document.querySelector('.row.justify-content-center');
            const items = Array.from(container.querySelectorAll('.col-md-4.mb-4'));

            items.sort((a, b) => {
                const priceTextA = a.querySelector('.new-price')?.innerText || '';
                const priceTextB = b.querySelector('.new-price')?.innerText || '';

                const priceA = priceTextA.includes('COMING SOON') ? 0 : parseFloat(priceTextA.replace(/[^\d.]/g, ''));
                const priceB = priceTextB.includes('COMING SOON') ? 0 : parseFloat(priceTextB.replace(/[^\d.]/g, ''));

                // COMING SOON luôn ở cuối
                if (priceA === 0 && priceB !== 0) return 1;
                if (priceB === 0 && priceA !== 0) return -1;
                if (priceA === 0 && priceB === 0) return 0;

                return sortOrder === 'asc' ? priceA - priceB : priceB - priceA;
            });

            items.forEach(item => container.appendChild(item));

            // Cập nhật tiêu đề dropdown
            document.getElementById('priceDropdownButton').textContent =
                sortOrder === 'asc' ? 'Giá: Thấp đến cao' : 'Giá: Cao đến thấp';
        });
    });
}
