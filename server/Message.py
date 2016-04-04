import webbrowser
from functools import partial

import server
import file_handler
import socket
import atexit
from Tkinter import *
from tkColorChooser import askcolor

_ip_error = 'No Wifi connection'
_max_speed = 19
_min_speed = 0
_max_sleep_time = 300
_min_sleep_time = 50
_general_width = 500

_file_name = 'imei'
_device1 = "352701060491419"
_device2 = "352701060478820"
_device3 = "352701060491294"
_device4 = "352701060477798"
_device5 = "352701060390926"
_device6 = "352701060477830"
_device7 = "352701060491450"
_device8 = "352701060219562"
_device9 = "352701060491484"
_device10 = "352701060491492"
_imeis_list = [_device1, _device2, _device3, _device4, _device5, _device6, _device7, _device8, _device9, _device10]


def get_color():
    global text_color
    color = str(askcolor())
    if 'None' not in color:
        color = color.split("'")
        text_color = color[1]
        refresh_color_text()


def get_ip_address():
    s = socket.socket(socket.AF_INET, socket.SOCK_DGRAM)
    try:
        s.connect(('8.8.8.8', 80))
    except socket.error:
        return _ip_error
    return s.getsockname()[0]


def refresh_ip_address():
    global host
    host = get_ip_address()
    if host == _ip_error:
        ip_label_text = _ip_error
        button_connect['state'] = DISABLED
    else:
        ip_label_text = 'Your IP is ' + host
        button_connect['state'] = NORMAL
    ip_label['text'] = ip_label_text


def refresh_sleep_time_text():
    return 'Text speed (min:' + str(_min_speed+1) \
           + ', max:' + str(_max_speed+1) + ') - Current is ' + str(text_speed+1)


def refresh_color_text():
    color_display['text'] = text_color
    color_display['bg'] = text_color


def update_speed():
    global text_speed
    entry = sleep_time_entry.get()
    if len(entry) == 0:
        sleep_time_error['text'] = 'Text speed cannot be empty'
    else:
        if re.match('^[0-9]*$', entry):
            speed = int(entry) - 1
            if _max_speed >= speed >= _min_speed:
                text_speed = speed
                sleep_time_label['text'] = refresh_sleep_time_text()
                sleep_time_error['text'] = ''
            else:
                sleep_time_error['text'] = 'Value is invalid'
        else:
            sleep_time_error['text'] = 'Entry is invalid'


def launch_server():
    server.start_server(_host, _port, _imeis_list)
    button_connect['text'] = 'Close server'
    button_connect['command'] = close_server


def close_server():
    server.stop_server()
    button_connect['text'] = 'Open server'
    button_connect['command'] = launch_server


def send_message():
    message = str(message_entry.get())
    if len(message) == 0:
        message_error['text'] = 'Message cannot be empty'
    else:
        if re.match('^[a-zA-Z0-9\s]*$', message):
            message_error['text'] = ''
            sleep_time = int(_max_sleep_time - (text_speed * ((_max_sleep_time-_min_sleep_time)/_max_speed)))
            message_handler.send_message(message + '\n', sleep_time, text_color)
        else:
            message_error['text'] = 'Message can only contain alphanumerical characters'


def imei_window():
    webbrowser.open(_file_name)


def refresh_imeis():
    global _imeis_list
    _imeis_list = file_handler.get_content(_file_name)


def exit_application():
    server.stop_server()


def init():
    if not file_handler.exists(_file_name):
        file_handler.write(_file_name, _imeis_list)

    if file_handler.empty(_file_name):
        file_handler.write(_file_name, _imeis_list)
    else:
        refresh_imeis()

    atexit.register(exit_application)


init()
message_handler = server.MessageHandler()

_port = 8080
_host = get_ip_address()

text_speed = int((_max_speed+1)/2) - 1
text_color = '#edc757'

root = Tk()
root.title('Message Display')
root.resizable(width=FALSE, height=FALSE)

layout = Frame(root, width=_general_width, padx=10, pady=10)
layout.pack()

header_frame = Frame(layout, relief=SUNKEN)
header_frame.pack(side=TOP)

ip_label = Label(header_frame)
ip_label.pack(side=LEFT)

button_refresh_ip = Button(header_frame, text='Refresh', command=refresh_ip_address)
button_refresh_ip.pack(side=RIGHT)

server_frame = Frame(layout, width=_general_width)
server_frame.pack(padx=10, pady=10, ipadx=5, ipady=5)

button_connect = Button(server_frame, text='Launch server', state=NORMAL, command=launch_server)
button_connect.grid(row=0, column=0, rowspan=2)

refresh_ip_address()

imei_frame = Frame(layout, width=_general_width)
imei_frame.pack(padx=10, pady=10, ipadx=5, ipady=5, side=BOTTOM)

imei_button = Button(imei_frame, text='Change Imeis', command=imei_window)
imei_button.grid(row=0, column=0)

imei_refresh = Button(imei_frame, text='Refresh List', command=refresh_imeis)
imei_refresh.grid(row=0, column=1)

settings_frame = Frame(layout)
settings_frame.pack(side=BOTTOM)

message_label = Label(settings_frame, text='Message:')
message_label.grid(row=0, column=0, sticky=W)

message_error = Label(settings_frame, fg='red')
message_error.grid(row=0, column=1, columnspan=2, sticky=W)

message_entry = Entry(settings_frame)
message_entry.grid(row=1, column=0, columnspan=2, sticky=W+E)

message_button = Button(settings_frame, text='Send', state=NORMAL, command=send_message)
message_button.grid(row=1, column=2, sticky=W+E)

color_label = Label(settings_frame, text='Choose a color - Current is ')
color_label.grid(row=2, column=0, sticky=W)

color_display = Label(settings_frame, bg=text_color, text=text_color)
color_display.grid(row=2, column=1, sticky=W)

color_button = Button(settings_frame, text='Update', command=get_color)
color_button.grid(row=2, column=2)

sleep_time_label = Label(settings_frame, text=refresh_sleep_time_text())
sleep_time_label.grid(row=3, column=0, sticky=W)

sleep_time_error = Label(settings_frame, fg='red')
sleep_time_error.grid(row=3, column=1, columnspan=2, sticky=W+E)

sleep_time_entry = Entry(settings_frame)
sleep_time_entry.grid(row=4, column=0, columnspan=2, sticky=W+E)

sleep_time_button = Button(settings_frame, text='Update', command=update_speed)
sleep_time_button.grid(row=4, column=2)

root.mainloop()
