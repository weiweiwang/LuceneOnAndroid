from __future__ import division
import math
import sys
import re
import os
import urllib
import smtplib
from email.mime.text import MIMEText
from email.mime.image import MIMEImage
from email.mime.multipart import MIMEMultipart
from email.MIMEBase import MIMEBase
from email import Encoders
from optparse import OptionParser
from datetime import datetime
import time
from datetime import timedelta
import sys
phone_number_pattern=re.compile("^(\\+?("+ "86|886|12520|12593|17950|17910|17900|17901|17951|17911|17909|12520" +")?)(0?\\d+)$")
mobile_pattern=re.compile("^[1][358][0-9]{9}$")
landline_pattern=re.compile("^(0\\d{2,3}-?)?\\d{7,8}(-\\d{3,4})?$")

def normalizePhone(phone):
    phone = re.sub("[^+\\d]","",phone)
    m = phone_number_pattern.match(phone)
    if m:
        phone=m.group(3)
    if mobile_pattern.match(phone):
        return phone
    else:
        return formatLandlinePhone(phone)

def formatLandlinePhone(phone):
    if landline_pattern.match(phone):
        c=phone[1]
        if c=='1'or c=='2':
            return "%s-%s" %(phone[0:3],phone[3:])
        else:
            return "%s-%s" %(phone[0:4],phone[4:])
    return None


if __name__ == '__main__':
	name=""
	phone=""
	for line in sys.stdin: 
		line=line.replace("\n","").replace("\r","")
		if line.startswith("TEL"):
			sp=line.split(":")
			phone=sp[len(sp)-1]
			phone=normalizePhone(phone)
		if line.startswith("FN"):
			name=line.split(":")[1]
		if line.startswith("END:VCARD") and name and phone:
			print "%s\t%s" %(name,phone)
			name=""
			phone=""

