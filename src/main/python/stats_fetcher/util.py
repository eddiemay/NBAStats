import os
import requests


zenrows_api_key = {}


def get_zenrows_api_key() -> str:
  if zenrows_api_key['value'] is None:
    with open('../../../../data/zenrows_api_key.txt', 'r') as f:
      zenrows_api_key['value'] = f.readline()

  return zenrows_api_key['value']


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
    'apikey': get_zenrows_api_key(),
  }
  response = requests.get('https://api.zenrows.com/v1/', params=params)
  # print(response.text)
  return response
