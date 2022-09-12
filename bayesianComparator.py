import sys
import numpy as np
import baycomp as bay


def main(listM1, listM2, rop):
    m1 = np.fromstring(listM1, dtype=float, sep=';')
    m2 = np.fromstring(listM2, dtype=float, sep=';')
    #r = bay.two_on_single(m1[0:np.size(m1)-1], m2[0:np.size(m2)-1], rope=rop)
    r = bay.SignedRankTest.probs(m1[0:np.size(m1)-1], m2[0:np.size(m2)-1], rope=rop)
    aux = np.asarray(r)
    f.write(np.array_str(aux)+"\n")


if __name__ == '__main__':
    path = sys.argv[1]
    numRope = int(sys.argv[2])

    f = open(path + "metricsJava.txt", "r")
    lineas = f.readlines()
    f.close()
    f = open(path + 'metricsPython.txt', 'w')

    for i in  range(0,len(lineas)):
        exp1 = lineas[i]

        for j in  range(0,len(lineas)):
            exp2 = lineas[j]

            if j != i:
                main(exp1, exp2, numRope)

    f.close()