import httpx
import json

url = "http://127.0.0.1:8000/identify"
image_path = r"C:\Users\vello\OneDrive\Desktop\GODREJ HACKATHOM\dataset\lantana - Google Search\lantana_test.jpg"

print(f"Testing API at {url}")
try:
    with open(image_path, "rb") as f:
        files = {"image": ("lantana_test.jpg", f, "image/jpeg")}
        # Increase timeout because ML inference + Groq API can take a few seconds
        response = httpx.post(url, files=files, timeout=60.0)
        print("Status code:", response.status_code)
        print("Response:", json.dumps(response.json(), indent=2))
except Exception as e:
    print("Error:", e)
