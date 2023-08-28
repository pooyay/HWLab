from fastapi import FastAPI, Request
# import uvicorn
# from pydantic import BaseModel
# from users import User
from starlette.responses import HTMLResponse
import time

import users

app = FastAPI()
socket_status = False
api_key = "oprfjiorghioherioghioerhgio345pw98ty"
last_esp_request = 0


def esp_connected():
    global last_esp_timestamp
    return time.time() <= last_esp_timestamp + 30


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


def authorize(path: str, req: Request):
    global api_key
    if path == '/socket_status':
        if 'Authorization' not in req.headers:
            raise Exception('Authorization header is required')
        if req.headers['Authorization'] != api_key:
            raise Exception('Authorization header is not valid')


# ESP
@app.post("/socket_status", response_class=HTMLResponse)
def update_socket_status(req: Request, status: str):
    authorize('/socket_status', req)
    global last_esp_timestamp
    last_esp_timestamp = time.time()
    global socket_status
    if status != 'no_change':
        socket_status = (status == 'on')
    time.sleep(10)
    return str(1 if socket_status else 0)


# USER
@app.get("/on")
def user_on():
    global socket_status
    if not esp_connected():
        return {"result": "fail", 'error': 'قطعه به اینترنت متصل نیست'}
    socket_status = True
    return {"result": "ok", 'state': socket_status}


# USER
@app.get("/off")
def user_off():
    global socket_status
    if not esp_connected():
        return {"result": "fail", 'error': 'قطعه به اینترنت متصل نیست'}
    socket_status = False
    return {"result": "ok", 'state': socket_status}


# USER
@app.get("/state")
def user_state():
    global socket_status
    if not esp_connected():
        return {"result": "fail", 'error': 'قطعه به اینترنت متصل نیست'}
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
