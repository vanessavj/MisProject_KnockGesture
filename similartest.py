Gestures = [("play", [0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0]),
                ("next", [0, 0, 1, 1, 0, 0, 0, 0, 1, 1, 0]),
                ("stop", [0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0])]

def main():

    testcases = [
                ("play", [0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0]),
                ("play", [1, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0]),
                ("play", [0, 1, 1, 0, 1, 0, 0, 0, 1, 0, 0]),
                ("play", [1, 1, 1, 0, 1, 0, 0, 0, 1, 0, 0]),
                ("play", [0, 1, 0, 0, 1, 1, 0, 0, 1, 0, 0]),
                ("play", [0, 1, 1, 1, 1, 0, 0, 0, 1, 0, 0]),
                ("play", [0, 1, 1, 0, 1, 1, 0, 0, 1, 0, 0]),
                ("play", [0, 1, 0, 0, 1, 0, 0, 0, 1, 1, 0]),
                ("play", [0, 1, 0, 0, 1, 0, 0, 0, 1, 0, 0]),
                ("play", [0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0]),
                ("play", [0, 1, 1, 0, 1, 1, 0, 0, 1, 1, 0]),
                ("play", [0, 1, 0, 0, 1, 0, 0, 0, 1, 1, 0]),
                ("play", [0, 1, 0, 1, 1, 1, 0, 0, 1, 0, 0]),
                ("play", [0, 1, 0, 1, 1, 1, 0, 0, 1, 1, 0]),
                ("play", [0, 1, 0, 0, 1, 0, 0, 1, 1, 1, 0]),
                ("play", [0, 1, 0, 0, 1, 0, 0, 0, 1, 1, 1]),
                ("play", [0, 1, 1, 0, 1, 0, 0, 1, 1, 0, 0]),
                # NEXT
                ("next", [0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0]),
                ("next", [0, 0, 1, 0, 0, 0, 0, 0, 1, 0, 0]),
                ("next", [0, 0, 0, 1, 0, 0, 0, 0, 0, 1, 0]),
                ("next", [0, 0, 0, 1, 0, 0, 0, 0, 1, 0, 0]),
                ("next", [0, 0, 1, 0, 0, 0, 0, 0, 1, 1, 0]),
                ("next", [0, 0, 0, 1, 0, 0, 0, 0, 1, 1, 0]),
                ("next", [0, 0, 1, 1, 0, 0, 0, 0, 0, 1, 0]),
                ("next", [0, 0, 1, 1, 0, 0, 0, 0, 1, 0, 0]),
                ("next", [0, 0, 1, 1, 0, 0, 0, 1, 1, 0, 0]),
                ("next", [0, 0, 1, 1, 0, 0, 0, 1, 1, 1, 0]),
                ("next", [0, 1, 1, 1, 0, 0, 0, 0, 1, 1, 0]),
                ("next", [0, 1, 1, 0, 0, 0, 0, 0, 1, 1, 0]),
                ("next", [1, 1, 0, 0, 0, 0, 0, 1, 1, 1, 0]),
                #STOP
                ("stop", [0, 1, 1, 1, 1, 1, 1, 1, 1, 1, 0]),
                ("stop", [1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1]),
                ("stop", [0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 0]),
                ("stop", [0, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0]),
                ("stop", [0, 0, 1, 1, 1, 1, 1, 1, 1, 0, 0]),
                ("stop", [0, 1, 1, 1, 1, 1, 1, 0, 1, 1, 0]),
                ("stop", [0, 1, 1, 0, 1, 1, 1, 1, 1, 1, 0]),
                ("stop", [1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 0]),
                ("stop", [0, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1]),
                ("stop", [1, 0, 1, 1, 1, 1, 1, 1, 1, 1, 1]),
                ("stop", [1, 1, 1, 1, 1, 1, 1, 1, 1, 0, 1]),
                ("stop", [1, 1, 1, 1, 1, 0, 1, 1, 1, 1, 1])
                ]

    count = 0
    for test in testcases:
        if not test_fn(test[1], test[0]):
            count += 1
    print(count, "tests failed (Total number of tests:", len(testcases), ")")

def find(term):
    for g in Gestures:
        if g[0] == term:
            return g[1]
    return "NOT FOUND"

def test_fn(input, expected):
    best = 1000000000
    best_gest = ""
    for g in Gestures:
        sim = isSimilar(g[1], input)
        if sim < best:
            best = sim
            best_gest = g[0]
    ret = best_gest == expected
    if ret:
        return True
    else:
        print()
        print()
        print("Test \033[0;31mfailed\033[0m!")
        print("Given:", input)
        print("Expected:", expected, find(expected))
        print("Found:", best_gest, find(best_gest))
        print()
        print()


def hammingweight(input):
    return input.count(1)


def getIndices(input):
    lst = []
    for i in input:
        if i == 1:
            lst.append(i)
    return lst

def siggi(distance):
    if distance <= 1:
        return 1
    else:
        return 2


def isSimilar(gesture, input):
    xor = []
    for i in range(len(input)):
        xor.append(input[i] ^ gesture[i])
    k = 2 * hammingweight(xor)
    if k == 0:
        return 0
    gesture_indices = getIndices(gesture)
    input_indices = getIndices(input)

    if hammingweight(input) == 0:
        return 1000000000000

    p = 1
    q = 1
    for i in range(len(input_indices) - 1):
        p *= siggi(input_indices[i+1] - input_indices[i])


    for i in range(len(gesture_indices) - 1):
        q *= siggi(gesture_indices[i+1] - gesture_indices[i]) 

    p = abs(p-q) + 1

    h = int(0.75 * len(gesture_indices))
    p += abs(gesture_indices[0] - input_indices[0])

    return k * p + h


if __name__ == '__main__':
    main()