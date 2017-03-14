import nfqueue
from scapy.all import *
import os
from random import choice
from string import ascii_uppercase




def setSystem():
    iptablesr = "iptables -A INPUT -j NFQUEUE --queue-num 1"
    os.system(iptablesr)
    #os.system("sysctl net.ipv4.ip_forward=1")


def resetSystem():
    os.system('iptables -F')
    os.system('iptables -X')


def callback(x, payload):
    #print payload
    data = payload.get_data()
    pkt = IP(data)
    if pkt.src == "127.0.0.1" and pkt[IP].dport == 8080:
        r = pkt.copy()
        payload_before = len(r[TCP].payload)
        regex = r"(?<=<d:resume xmlns:d=\"http://demo\">)(.*)(?=</d:resume)"
        matches = re.search(regex, str(r[TCP].payload), re.MULTILINE)
        if matches:
            print matches.group(0)
            print len(matches.group(0))
            result = ''.join(choice(ascii_uppercase) for i in range(len(matches.group(0))))
            print len(result)
            r[TCP].payload = re.sub(regex, result, str(pkt[TCP].payload), 1, re.MULTILINE)
            print r[TCP].payload

        payload_after = len(r[TCP].payload)
        #print "AFTER: " + str(payload_after)

        payload_dif = payload_after - payload_before

        r[IP].len = pkt[IP].len + payload_dif



        payload.set_verdict_modified(nfqueue.NF_ACCEPT, str(r), len(r))
        print "PASSOU"
        #payload.set_verdict_modified(nfqueue.NF_ACCEPT, str(pkt), len(pkt))

        #Now Edit the packet and change the content
        #payload.set_verdict_modified(nfqueue.NF_ACCEPT, str(packet), len(packet))
        #payload.set_verdict(nfqueue.NF_DROP)
    else:
        #Let the other packets go
        payload.set_verdict(nfqueue.NF_ACCEPT)


def main():
    setSystem()
    # This is the intercept
    q = nfqueue.queue()
    q.open()
    q.bind(socket.AF_INET)
    q.set_callback(callback)
    q.create_queue(1)
    try:
        q.try_run() # Main loop
    except KeyboardInterrupt:
        q.unbind(socket.AF_INET)
        q.close()
        resetSystem()


if __name__ == "__main__":
    main()