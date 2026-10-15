from datetime import datetime
import threading

class Logger:
    _lock = threading.Lock()

    @classmethod
    def log(cls, message: str) -> None:
        with cls._lock:
            print(f"|{datetime.now().isoformat()}|:   {message}", flush=True)
