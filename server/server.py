from fastapi import FastAPI
# import uvicorn
# from pydantic import BaseModel
# from users import User
from starlette.responses import HTMLResponse

import users

app = FastAPI()
socket_status = False
esp_connected = False


@app.get("/", response_class=HTMLResponse)
def running_server():
    html = "<html><body>"
    html += "<h1>ESP8266 Relay Control</h1>"
    html += "<p>Click <a href='/state'>here</a> to get currect state.</p>"
    html += "<p>Click <a href='/on'>here</a> to turn the relay ON.</p>"
    html += "<p>Click <a href='/off'>here</a> to turn the relay OFF.</p>"
    # html += "<p>Click <a href='/wifi'>here</a> to configure WiFi.</p>"
    html += "</body></html>"
    return html


# ESP
@app.post("/socket_status", response_class=HTMLResponse)
def update_socket_status(status: str):
    global socket_status
    if status != 'no_change':
        socket_status = (status == 'on')
    return str(1 if socket_status else 0)


# USER
@app.get("/on")
def user_on():
    global socket_status
    socket_status = True
    return {"result": "ok", 'state': socket_status}


# USER
@app.get("/off")
def user_off():
    global socket_status
    socket_status = False
    return {"result": "ok", 'state': socket_status}


# USER
@app.get("/state")
def user_state():
    global socket_status
    return {"result": "ok", 'state': socket_status}


# USER
@app.post("/change_status")
def authenticate_user(status, user_info):
    global esp_connected

    if not esp_connected:
        return {"message": "ESP8266 is not connected"}

    username, password = user_info['username'], user_info['password']

    if users.User.authentication(username, password):
        global socket_status
        socket_status = status
        return {"message": "Socket status changed successfully"}
    else:
        return {"message": "Authentication failed"}


@app.get("/get_socket_status")
def get_socket_status():
    global socket_status
    if socket_status:
        return {"socket status is": "ON"}
    else:
        return {"socket status is": "OFF"}
