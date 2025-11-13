import os
import requests


def is_int(s: str):
  if s is None:
    return False
  try:
    int(s)
    return True
  except ValueError:
    return False


def is_float(s: str):
  if s is None:
    return False
  try:
    float(s)
    return True
  except ValueError:
    return False


def is_file_exist(file: str):
  return os.path.exists(file) and os.path.getsize(file) != 0


def send_request(url: str) -> requests.Response:
  # time.sleep(1) # Sleep for 1 second so we don't get blacklisted.
  params = {
    'url': url,
    'apikey': 'c32e7716f3998f6d5d2717caff3ca1e031bb749e',
  }
  response = requests.get('https://api.zenrows.com/v1/', params=params)
  # print(response.text)
  return response
