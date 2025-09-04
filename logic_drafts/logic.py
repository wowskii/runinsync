import requests
import os
from dotenv import load_dotenv

load_dotenv()

BASE_URL="https://api.getsong.co/"
API_KEY = os.getenv("BPM_API_KEY")

steps_per_minute = 180



def get_bpm(song,artist):
    response = requests.get(
        f"{BASE_URL}search/",
        params={
            "type": "both",
            "lookup": f"song:{song}artist:{artist}",
            "limit": 5,
            "api_key": API_KEY
        },
    )
    #print(response.json()['search']['title'])
    return response.json()['search'][0]['tempo']

#print(get_bpm("Gold Guns Girls", "Metric"))

def calculate_song_bpm_factor(song_bpm, user_spm=steps_per_minute):
    while user_spm > 2*song_bpm:
        song_bpm *= 2
    return user_spm/song_bpm

print(calculate_song_bpm_factor(89))