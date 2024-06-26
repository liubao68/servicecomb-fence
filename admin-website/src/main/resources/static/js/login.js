/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

function loginAction() {
     var username = document.getElementById("username").value;
     var password = document.getElementById("paasword").value;
     var formData = {};
     formData.username = username;
     formData.password = password;
     formData.grant_type = "password";

     $.ajax({
        type: 'POST',
        url: "/v1/token",
        data: formData,
        success: function (data) {
            console.log(JSON.stringify(data));
            window.localStorage.setItem("token", JSON.stringify(data));
            window.location = "/ui/admin/operation.html";
        },
        error: function(data) {
            console.log(JSON.stringify(data));
            var error = document.getElementById("error");
            error.textContent="Login failed";
            error.hidden=false;
        },
        async: true
    });
}

function loginWithGithubAction() {
    setCookie("initialState", Math.floor(100000000 + Math.random() * 900000000), 1);
    var redirectURI = window.location.protocol + "//" 
      + window.location.hostname + ":" + window.location.port
      + "/ui/admin/githubLoginCallback.html";
    redirectURI = encodeURIComponent(redirectURI);
    
    $.ajax({
        type: 'GET',
        url: "/api/authentication/v1/thirdParty/providerInfo/github?redirectURI=" + redirectURI,
        success: function (data) {
            console.log(JSON.stringify(data));
            window.location = data;
        },
        error: function(data) {
            console.log(JSON.stringify(data));
            var error = document.getElementById("error");
            error.textContent="Login failed";
            error.hidden=false;
        },
        async: true
    });
}

/*
*  https://www.w3schools.com/js/js_cookies.asp 
*/
function setCookie(name,value,days) {
    var expires = "";
    if (days) {
        var date = new Date();
        date.setTime(date.getTime() + (days*24*60*60*1000));
        expires = "; expires=" + date.toUTCString();
    }
    document.cookie = name + "=" + (value || "")  + expires + "; path=/";
}

/*
*  https://www.w3schools.com/js/js_cookies.asp 
*/
function getCookie(cname) {
  var name = cname + "=";
  var decodedCookie = decodeURIComponent(document.cookie);
  var ca = decodedCookie.split(';');
  for(var i = 0; i <ca.length; i++) {
    var c = ca[i];
    while (c.charAt(0) == ' ') {
      c = c.substring(1);
    }
    if (c.indexOf(name) == 0) {
      return c.substring(name.length, c.length);
    }
  }
  return "";
}