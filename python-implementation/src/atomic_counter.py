import threading
class AtomicCounter:
    def __init__(self, value=0): self._value=value; self._lock=threading.Lock()
    def increment(self, n=1):
        with self._lock: self._value+=n; return self._value
    def decrement(self, n=1):
        with self._lock: self._value-=n; return self._value
    def get(self):
        with self._lock: return self._value
