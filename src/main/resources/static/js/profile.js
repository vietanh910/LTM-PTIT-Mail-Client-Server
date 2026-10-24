(() => {
    const form = document.getElementById('profile-form');
    const fileInput = document.getElementById('avatar');
    const preview = document.getElementById('avatar-preview');
    const removeAvatar = document.getElementById('remove-avatar');
    const error = document.getElementById('avatar-error');
    const originalAvatar = preview.src;
    let previewUrl;

    function clearPreview() {
        if (previewUrl) URL.revokeObjectURL(previewUrl);
        previewUrl = undefined;
    }

    fileInput.addEventListener('change', () => {
        error.textContent = '';
        clearPreview();
        const file = fileInput.files[0];
        preview.src = removeAvatar.checked ? '/image/user.jpeg' : originalAvatar;
        if (!file) return;
        if (!['image/jpeg', 'image/png'].includes(file.type) || file.size > 2 * 1024 * 1024) {
            error.textContent = 'Vui lòng chọn ảnh JPG hoặc PNG không quá 2 MB.';
            fileInput.value = '';
            return;
        }
        removeAvatar.checked = false;
        previewUrl = URL.createObjectURL(file);
        preview.src = previewUrl;
    });

    removeAvatar.addEventListener('change', () => {
        clearPreview();
        fileInput.value = '';
        error.textContent = '';
        preview.src = removeAvatar.checked ? '/image/user.jpeg' : originalAvatar;
    });

    const currentPassword = document.getElementById('currentPassword');
    const newPassword = document.getElementById('newPassword');
    const confirmPassword = document.getElementById('confirmPassword');
    function validatePassword() {
        const changing = Boolean(currentPassword.value || newPassword.value || confirmPassword.value);
        currentPassword.required = changing;
        newPassword.required = changing;
        confirmPassword.required = changing;
        confirmPassword.setCustomValidity(newPassword.value !== confirmPassword.value ? 'Xác nhận mật khẩu mới không khớp.' : '');
    }
    [currentPassword, newPassword, confirmPassword].forEach(input => input.addEventListener('input', validatePassword));

    form.addEventListener('submit', event => {
        validatePassword();
        if (!form.reportValidity()) {
            event.preventDefault();
            return;
        }
        const button = document.getElementById('save-profile');
        button.disabled = true;
        button.textContent = 'Đang lưu...';
    });
    window.addEventListener('pageshow', () => {
        document.getElementById('save-profile').disabled = false;
        document.getElementById('save-profile').textContent = 'Lưu thay đổi';
        validatePassword();
    });
    window.addEventListener('pagehide', clearPreview);
})();
