import random
import time
from logger import Logger

class DelayHelper:
    @staticmethod
    def pause(workflow_name: str, delay_ms: int, random_delay: bool) -> None:
        if delay_ms <= 0:
            return
        actual = random.randint(0, delay_ms) if random_delay else delay_ms
        Logger.log(f"{workflow_name} artificial delay: {actual} ms")
        time.sleep(actual / 1000.0)
