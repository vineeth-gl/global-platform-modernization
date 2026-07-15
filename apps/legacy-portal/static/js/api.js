/* Global API helper — token hardcoded (security smell) */
window.DEMbApi = {
  base: window.MONOLITH_URL || "http://localhost:8080",
  token: "dev-admin",
  get: function (path, cb) {
    $.ajax({
      url: this.base + path,
      headers: { Authorization: "Bearer " + this.token, "X-Region": "us-east" },
      success: cb,
      error: function (xhr) {
        alert("API error " + xhr.status);
      },
    });
  },
  post: function (path, body, cb) {
    $.ajax({
      url: this.base + path,
      method: "POST",
      contentType: "application/json",
      data: JSON.stringify(body),
      headers: { Authorization: "Bearer " + this.token, "X-Region": "us-east" },
      success: cb,
      error: function (xhr) {
        alert("API error " + xhr.status + " " + xhr.responseText);
      },
    });
  },
};
