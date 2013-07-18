# Introduction

The Job Map is a RESTful web service. This document describes our API interface and how you can implement services to interact with our service.


## Note on high-replication datastore

The Job Map uses the High-Replication Datastore (HRD) available in Google App Engine. This is a distributed replication database. Due to this there may be considerable delay from when posting new information to the server to when it is available for fetching (up to tens of seconds in worst cases). Watch [this video](http://youtu.be/xO015C3R6dw) to learn more.

Not all queries are affected by this delay. In fact, only a few queries are. The queries that are not affected by this are called "ancestor queries". Nonetheless, it is good practice to always assume that some delay exists.

The Job Map client uses smart caching to hide this delay from the user. In addition, the backend code uses logic to generate entity keys in a way that instead of adding duplicate entries, new entries replace older entries when replicating throughout the database.

Therefore, make sure that your application accounts for this delay, and does not request objects shortly after they have been submitted. If the submission was returned with an "Ok"-response, then it was successfully added to the database.


# URLs

Base url: `http://thejobmap.appspot.com/rest/`

Available servlets:
  * `/user/`
  * `/openid/`
  * `/marker/`
  * `/apply/`



## `/user/` resource

Manages users of The Job Map.

### `GET /user/`

Fetches list of all users, including their details. Restricted to admins.

### `GET /user/<email>`

Gets user details.

`<email>` can be substituted with "`me`" to get your details. Normal user is only granted access to their own details.

### `GET /user/<email>/cv`

Download user CV. Response is a pdf file. Restricted access.

### `GET /user/<email>/cv/uploadUrl`

Gets an upload url to upload CV to server.

Send pdf file to the returned url to store it on the server. The field name for the file must be `cv`. Max filesize is 1 MB.

### `POST /user/<email>`

Updates user details. `<email>` can be substituted with `me` to update your details. Normal user is only granted access to their own details.

### `DELETE /user/<email>`

Delete user profile and any markers or other data associated with the user. Restricted to admins.

### `DELETE /user/<email>/cv`

Delete user CV.



## `/openid/` resource

Manages OpenID login requests.

### `GET /openid/`

Fetches urls to allowed OpenID providers.



## `/marker/` resource

Manages markers in The Job Map.

### `GET /marker/`

Fetches list of all markers.

### `POST /marker/`

Add a new marker to the database. A normal user is only allowed to have one marker.

### `GET /marker/<type>`

Fetch list of markers with specified type. `<type>` can be `city`, `company` or `random`.

### `GET /marker/<id>`

Gets marker details. `<id>` can be substituted with `<email>` or `me` to get your marker.

### `POST /marker/<id>`

Update the details of a specific marker. `<id>` can be substituted with `<email>` or `me` to update your marker.

### `DELETE /marker/<id>`

Delete the marker.



## `/apply/` resource

Apply for jobs.

### `POST /apply/<id>`

Apply for the job. `<id>` refer to the marker that represents the job.



# JSON objects

## Result object

```
result = {
  result: string,
  info: string,
  id: long,
}
```

The result object is sent in response to a request.

Example:
```
POST /marker/
{ lat:13.37, lng:13.37, title:"ICA Porsön", info:"Söker en kassörska" }
```
```
{ result:"ok", id:34 }
```

This means the operation was successful. The id of the new marker is `34`, and this can be used if further manipulation of the object is necessary. The info property is used if more information is available (often when an error has occurred, in which case result will be `"fail"`).


## Marker object

```
marker = {
  id: long,
  lat: double,
  lng: double,
  type: string,
  cat: string,
  title: string,
  info: string,
  author: string,
  privacy: string,
  creationDate: long,
  updatedDate: long,
  numApply: long,
}
```

This object is exchanged between server and client when adding new markers and fetching markers from the server.

  * The `id` property can be used to uniquely identify a marker in the database.
  * The `lat` and `lng` properties indicate position on the map.
  * The `type` property can be either `city`, `company` or `random`.
  * The `cat` property indicate a category and can be one of several pre-defined categories (see source code).
  * The `title` property is the title for the marker.
  * The `info` property is the breadcrumb text for the marker.
  * The `author` property contains the email address for the creator of the marker (*note:* this is only sent out to the owner of the marker so the client knows it can modify that marker).
  * The `privacy` property is specified for markers of type `random`, and says `"public"` if it's a public marker or `"private"` if the creator wants to keep the marker hidden for normal users.
  * The `creationDate` property is set to the current unix time when the marker is created.
  * The `updatedDate` property is updated every time the marker is updated.
  * The `numApply` property, only applicable for markers of type `company`, says how many times this job has been applied to.

These properties can not be sent to the server: `id`, `author`, `creationDate`, `updatedDate`, `numApply`.

Example:
```
GET /marker/
```
```
[
 { id:4, lat:12.34, lng:43.21, type:"company", cat:"administration", title:"ICA Porsön", info:"Vi söker en ny chef", creationDate:1325693323587, updatedDate:1325697323587 },
 { id:7, lat:34.12, lng:21.43, type:"company", cat:"administration", title:"LTU", info:"Vi behöver en ny informationsansvarig. Den som sitter just nu är så dålig på att skicka ut information om datum i god tid.", creationDate:1325693323587, updatedDate:1325697323587 }
]
```

Example:
```
POST /marker/34
{ lat:12.34, lng:43.21, type:"company", cat:"administration", title:"ICA Porsön", info:"Vi söker en ny chef. Du ska vara bra." }
```
```
{ result:"ok", id:34 }
```


## Apply object

```
apply = {
  motivation: string,
}
```

This object is sent when applying for a job.

Example:
```
POST /apply/34
{ motivation: "Jag kan hjälpa er styra upp verksamheten." }
```
```
{ result:"ok" }
```

Example:
```
POST /apply/1337
{ motivation: "Hallå. Är nån där?" }
```
```
{ result:"fail", info:"no such marker" }
```

## OpenID provider object

```
openIDProvider = {
  name: string,
  loginUrl: string,
}
```

This object is sent when the user is requesting login urls.

Example:
```
GET /openid/
```
```
[
 { name:"Google",   loginUrl:"http://thejobmap.appspot.com/_ah/login_redir?claimid=google.com/accounts/o8/id&continue=http://thejobmap.appspot.com/special/login" }
 { name:"myOpenID", loginUrl:"http://thejobmap.appspot.com/_ah/login_redir?claimid=myopenid.com&continue=http://thejobmap.appspot.com/special/login" }
 { name:"Yahoo",    loginUrl:"http://thejobmap.appspot.com/_ah/login_redir?claimid=yahoo.com&continue=http://thejobmap.appspot.com/special/login" }
]
```


## User object

```
user = {
  email: string,
  name: string,
  birthday: long,
  sex: string,
  phonenumber: string,
  privileges: string,
  cvUploaded: boolean,
  lastLogin: long,
  logoutUrl: string,
}
```

This object is exchanged between server and client when the client is fetching the user profile and when updating the user profile.

Example:
```
GET /user/me
```
```
{ email:"test@example.com", name:"example", birthday:515974400000, sex:"Male", phonenumber:"", privileges:"admin", cvUploaded:false, lastLogin:715974400000, logoutUrl:"http://thejobmap.appspot.com/_ah/openid_logout?continue=http://thejobmap.appspot.com/" }
```

Example:
```
POST /user/me
{ name:"Mr. Example", birthday:615974400000, sex:"Not telling", phonenumber:"123" }
```
```
{ result:"ok" }
```


## Upload url object

```
uploadUrl = {
  uploadUrl: string,
}
```

This object is sent when the server is giving the client an url for with which a file can be uploaded.

Example:
```
GET /user/me/cv/uploadUrl
```
```
{ uploadUrl:"http://thejobmap.appspot.com/_ah/upload/AMmfu6ZkG-DdOlnlMz1FsFmfM7Lf3xS_m3K_od1_n70fPSaLHQeZC2TTTEaNwRWIO6V_zbq11IBAnqM4bWfWzGDlgS4IZOprMDUs-9LZMGufWs3wGgKG_9cbypZAxb5aUwzIlabyJOaw0IbnMSRt5BsJWsqbbHgbsPfSuCkPHsfkoQKy1QpZ89w/ALBNUaYAAAAATwTBy2Vgvb_wmFxdEbgzKwZSRVdlBAsj/" }
```
