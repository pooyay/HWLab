
class User:
    all_users = {}

    def __init__(self, username, password):
        self.username = username
        self.password = password
        User.all_users[self.username] = self.password

    @staticmethod
    def authentication(username, password):
        if User.all_users[username] is None:
            print("There is no such username")
            return False
        if User.all_users[username] == password:
            return True
        else:
            return False


