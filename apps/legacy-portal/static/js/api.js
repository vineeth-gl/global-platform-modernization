/* Global API helper */
window.DEMbApi = {
  base: window.MONOLITH_URL || "http://localhost:8080",
  token: window.localStorage.getItem("dem_token") || "",
  headers: function () {
    var headers = { "X-Region": "us-east" };
    if (this.token) headers.Authorization = "Bearer " + this.token;
    return headers;
  },
  get: function (path, cb) {
    $.ajax({
      url: this.base + path,
      headers: this.headers(),
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
      headers: this.headers(),
      success: cb,
      error: function (xhr) {
        alert("API error " + xhr.status + " " + xhr.responseText);
      },
    });
  },
};
