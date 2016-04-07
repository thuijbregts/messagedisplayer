#!/usr/bin/python
import SocketServer
import socket
import time
from threading import *
from thread import start_new_thread


def get_device_position(imei):
    i = 0
    for item in imeis_list:
        item = str(item)
        if item.startswith(imei):
            return i
        i += 1
    return 0


def get_max_position():
    maximum = 0
    for cl in clients_list:
        position = cl.get_position()
        if position > maximum:
            maximum = position
    return maximum


def send_message_to_client(cl, message, color, max_latency, sleep_time):
    cl.send_message(message, color, max_latency, sleep_time)


def notify_disconnection_to_clients():
    for cl in clients_list:
        cl.server_offline()


class MessageHandler:
    def __init__(self):
        self.init = 1

    def send_message(self, message, sleep_time, text_color):
        if len(clients_list) > 0:
            max_latency = 0
            for cl in clients_list:
                cl_latency = cl.get_latency()
                if cl_latency > max_latency:
                    max_latency = cl_latency
            for cl in clients_list:
                start_new_thread(send_message_to_client, (cl, message, text_color, max_latency, sleep_time))


class Client(Thread):
    def __init__(self, sock, address):
        Thread.__init__(self)
        self.sock = sock
        self.addr = address
        self.running = 1
        self.pause = 0
        self.avg_latency = 0.0
        self.start()
        self.position = 0

    def run(self):
        while self.running == 1:
            try:
                data = str(self.sock.recv(1024))
                if data:
                    if data.startswith('ping'):
                        split_data = data.split('/')
                        latency = self.time_in_milliseconds() - long(split_data[2])
                        if self.avg_latency*2 >= latency >= self.avg_latency/2:
                            self.avg_latency = (self.avg_latency + latency) / 2
                        elif self.avg_latency == 0:
                            self.avg_latency = latency;
                        self.pause = 0
                    elif data == 'disconnected':
                        self.running = 0
                        self.disconnect()
                    else:
                        print data
                        self.position = get_device_position(data)
                        self.send_position(self.position)
            except IOError:
                self.running = 0

    def send_message(self, message, color, max_latency, sleep_time):
        delay = int(max_latency - self.avg_latency)
        string_to_send = str(get_max_position()+1) + '/' + color + '/' + str(delay) + '/' + str(sleep_time) + '/' + message
        self.sock.send(string_to_send)

    def send_position(self, position):
        string_to_send = 'position/' + str(position) + '\n'
        self.sock.send(string_to_send)

    def get_latency(self):
        self.avg_latency = 0.0
        for i in range(15):
            self.pause = 1
            string_to_send = 'ping/' + str(i) + '/' + str(self.time_in_milliseconds()) + '\n'
            self.sock.send(string_to_send)
            while self.pause == 1:
                time.sleep(0.01)
        return self.avg_latency

    def disconnect(self):
        self.sock.close()
        clients_list.remove(self)

    def server_offline(self):
        string_to_send = 'offline\n'
        self.sock.send(string_to_send)

    def get_position(self):
        return self.position

    @staticmethod
    def time_in_milliseconds():
        return int((time.time() + 0.5) * 1000)


def wait_for_connections():
    global server_online
    while server_online == 1:
        try:
            (conn, addr) = server_socket.accept()
            client = Client(conn, addr)
            clients_list.insert(len(clients_list), client)
            if server_online == 0:
                client.server_offline()
        except IOError:
            server_online = 0


def start_server(host, port, imeis):
    global server_socket
    global server_online
    global imeis_list
    imeis_list = imeis
    if server_online == 0:
        server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
        server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
        try:
            server_socket.bind((host, port))
        except SocketServer.socket.error:
            pass
        server_socket.listen(5)
        server_online = 1
        start_new_thread(wait_for_connections, ())


def stop_server():
    global server_online
    if server_online == 1:
        server_online = 0
        start_new_thread(notify_disconnection_to_clients, ())
        server_socket.shutdown(socket.SHUT_RDWR)
        server_socket.close()

clients_list = []
imeis_list = []
server_socket = socket.socket(socket.AF_INET, socket.SOCK_STREAM)
server_socket.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
server_online = 0
