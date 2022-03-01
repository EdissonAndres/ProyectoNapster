# -*- coding: utf-8 -*-
"""

@author: Edison Andres
"""
#Import jpysocket: pip install jpysocket
import jpysocket
import threading
import socket
import json
import re

TCP_IP = '127.0.0.1'
TCP_PORT = 5001
BUFFER_SIZE = 10240
LIST_DATA_SONGS = []

def search(atribute, data_search):
  list_results = []
  for song in LIST_DATA_SONGS:
    data = (song[atribute]).lower()
    find = data.find(data_search)
    if find != -1:
      list_results.append(song)
      
  json_results = json.dumps(list_results)
  return json_results


def save_data_songs(data_songs):
  json_file = json.loads(data_songs)
  LIST_DATA_SONGS.extend(json_file)
  LastPos = len(LIST_DATA_SONGS) - 1
  return LIST_DATA_SONGS[LastPos]
  

def delete_song(ip, port):
  songsToDeleted =[]
  for song in LIST_DATA_SONGS:
    if song["ip"] == ip and song["port"] == port:
      songsToDeleted.append(song)
  for s in songsToDeleted:
    LIST_DATA_SONGS.remove(s)
  
  

class Client_Thread(threading.Thread):
    def __init__(self, conn, addr):
        threading.Thread.__init__(self)
        self.conn = conn
        self.addr = addr
        self.ipServerClient = ""
        self.portServerClient = 0
        
    def run(self):
      while True:
        try:
          data = self.conn.recv(BUFFER_SIZE)
          try:
            data = jpysocket.jpydecode(data)
            if data[0] == "@":
              print("SEARCH REQUEST RECEIVED FROM %s: %d" % addr)
              parameters = re.split(r'@', data)
              results = search(parameters[1],parameters[2])
              results = results + '\n'
              try:
                self.conn.send(results.encode("UTF-8"))
                print("REPLAY SEND TO %s: %d" % addr)
              except:
                print("COULD NOT REPLY TO %s: %d" % addr)
                self.conn.close()
            elif data[0] == "[":
              dataClient = save_data_songs(data)
              self.ipServerClient = dataClient["ip"]
              self.portServerClient = dataClient["port"]
              print("SONGS DATA RECEIVED FROM %s: %d" % addr + ", TOTAL REGISTERS " + str(len(LIST_DATA_SONGS)) + " SONGS" )
              try:
                self.conn.send("RECEIVED\n".encode("UTF-8"))
              except:
                print("COULD NOT CONFIRM TO %s: %d" % addr)
                self.conn.close()
                break
          except:
            print("DATA INVALID RECEIVED FROM %s: %d" % addr)
            self.conn.send("DATA_INVALID\n".encode("UTF-8"))
        except:
          print("LOST CONNECTION WITH %s: %d" % addr)
          self.conn.close()
          delete_song(self.ipServerClient, self.portServerClient)
          print("SONGS DATA OF %s: %d" % addr + " DELETED, TOTAL REGISTERS " + str(len(LIST_DATA_SONGS)) + " SONGS" )
          break
      
  
if __name__ == "__main__":
  socket_server = socket.socket()
  socket_server.bind((TCP_IP, TCP_PORT))
  socket_server.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
  socket_server.listen(5)

  print ("SERVER LISTENING")

  while True:
    conn, addr = socket_server.accept()
    print("%s: %d CONNECTED" % addr)
    client = Client_Thread(conn, addr)
    client.start()
