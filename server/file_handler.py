import fileinput
import os
import re


def exists(file_name):
    if os.path.isfile(file_name):
        return True
    else:
        return False


def empty(file_name):
    if os.stat(file_name).st_size == 0:
        return True
    else:
        return False


def get_content(file_name):
    list_imei = []
    f = open(file_name, 'r+')
    for line in f:
        if len(line) > 1:
            if re.match('^[0-9]*$', line[0:len(line)-1]):
                list_imei.append(line)
    return list_imei


def write(file_name, list_imei):
    f = open(file_name, 'a')
    for imei in list_imei:
        f.write(imei + '\n')


def replace(file_name, old_imei, current_imei):
    for line in fileinput.input(file_name, inplace=True):
        print line.replace(old_imei, current_imei)
