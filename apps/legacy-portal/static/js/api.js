/* Global API helper — token from localStorage (MAD-116: no hardcoded secrets) */
window.DEMbApi = {
  base: window.MONOLITH_URL || "http://localhost:8080",
  getToken: function () {
    return localStorage.getItem("dem_token") || "";
  },
  get: function (path, cb) {
    var token = this.getToken();
    if (!token) {
      alert("Set localStorage.dem_token for API access (see README local dev setup)");
      return;
    }
    $.ajax({
      url: this.base + path,
      headers: { Authorization: "Bearer " + token, "X-Region": "us-east" },
      success: cb,
      error: function (xhr) {
        alert("API error " + xhr.status);
      },
    });
  },
  post: function (path, body, cb) {
    var token = this.getToken();
    if (!token) {
      alert("Set localStorage.dem_token for API access (see README local dev setup)");
      return;
    }
    $.ajax({
      url: this.base + path,
      method: "POST",
      contentType: "application/json",
      data: JSON.stringify(body),
      headers: { Authorization: "Bearer " + token, "X-Region": "us-east" },
      success: cb,
      error: function (xhr) {
        alert("API error " + xhr.status + " " + xhr.responseText);
      },
    });
  },
};
