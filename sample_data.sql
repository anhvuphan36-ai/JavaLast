USE JudgeSystem;

-- ============================================
-- BÀI 1: Alpha Country
-- ============================================
INSERT INTO Problems (title, description, time_limit, memory_limit, contest_type, checker_script)
VALUES (
    'Alpha Country',
    'Problem A. Alpha Country\nBallon:\nTime limit: 1 seconds\nMemory limit: 512 megabytes\nIn Alpha country there are n islands numbered from 1 to n. The islands are connected by a one-way bridge (island i can only go to island i + 1). On each island you arrive, you can receive a bonus or must pay a fine mi dollars (mi is a positive number represents the amount of money you will receive and mi is a negative number represents the amount of money you will must pay a fine) (The current your amount of money can be negative).\nTuan is given two times to use magic by a magician, one time can teleport to any island (use the first time go to Alpha country), go to the next island sequentially and one time can return to his home anytime. However, He will have to send back to the magician an amount equal to the largest amount was collected on an island that he arrived. Tuan will use magic optimally to earn the maximum amount of money.\nPrint the maximum possible amount of money Tuan can earn.\nInput\nThe first line contains a single integer n safety 1 <= n <= 10^5 - the number of islands in Alpha country.\nThe second line contains n integers m1,m2,...,mn safety -500 <= mi <= 500 - the amount of money you can receive a bonus or pay a fine in island i.\nOutput\nPrint a single integer - the maximum possible amount of money Tuan can earn.\nExamples\nstandard input\n5\n6 -5 7 3 -2\nstandard output\n4',
    1000, 512, 'ICPC',
    NULL
);
SET @p1 = LAST_INSERT_ID();

-- Testcases for Alpha Country
INSERT INTO Testcases (problem_id, input_data, expected_output, testcase_type, is_ai_generated) VALUES
(@p1, '1\n10\n', '0\n', 'small', TRUE),
(@p1, '5\n6 -5 7 3 -2\n', '4\n', 'normal', TRUE),
(@p1, '1\n-10\n', '0\n', 'edge', TRUE),
(@p1, '5\n-1 -2 -3 -4 -5\n', '0\n', 'anti-wa', TRUE),
(@p1, '2\n10 20\n', '10\n', 'small', TRUE),
(@p1, '10\n1 2 3 4 5 6 7 8 9 10\n', '45\n', 'normal', TRUE),
(@p1, '5\n-10 -20 -30 -40 -50\n', '0\n', 'edge', TRUE),
(@p1, '10\n-1 -1 -1 -1 -1 -1 -1 -1 -1 -1\n', '0\n', 'anti-wa', TRUE),
(@p1, '1\n1\n', '0\n', 'small', TRUE),
(@p1, '1\n-500\n', '0\n', 'edge', TRUE),
(@p1, '2\n1 1\n', '1\n', 'small', TRUE),
(@p1, '8\n-1 5 -4 3 4 6 -10 4\n', '8\n', 'normal', TRUE),
(@p1, '1\n500\n', '0\n', 'edge', TRUE),
(@p1, '5\n1 2 3 4 5\n', '10\n', 'anti-wa', TRUE);

-- Sample Codes for Alpha Country
INSERT INTO SampleCodes (problem_id, code_content, language, expected_type, is_ai_generated) VALUES
(@p1,
'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextInt();
        }
        long ans = 0;
        for (int l = 0; l < n; l++) {
            long sum = 0;
            int mx = Integer.MIN_VALUE;
            for (int r = l; r < n; r++) {
                sum += a[r];
                mx = Math.max(mx, a[r]);
                ans = Math.max(ans, sum - mx);
            }
        }
        System.out.println(ans);
    }
}',
'java', 'AC', FALSE);

INSERT INTO SampleCodes (problem_id, code_content, language, expected_type, is_ai_generated) VALUES
(@p1,
'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextInt();
        }
        long ans = 0;
        for (int l = 0; l < n; l++) {
            long sum = 0;
            int mx = Integer.MIN_VALUE;
            // BUG: r luôn bắt đầu từ 0
            for (int r = 0; r < n; r++) {
                sum += a[r];
                mx = Math.max(mx, a[r]);
                ans = Math.max(ans, sum - mx);
            }
        }
        System.out.println(ans);
    }
}',
'java', 'WA', FALSE);

INSERT INTO SampleCodes (problem_id, code_content, language, expected_type, is_ai_generated) VALUES
(@p1,
'import java.util.*;

public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextInt();
        }
        long ans = 0;
        // O(n^3) - quá chậm với n lớn
        for (int l = 0; l < n; l++) {
            for (int r = l; r < n; r++) {
                long sum = 0;
                int mx = Integer.MIN_VALUE;
                for (int k = l; k <= r; k++) {
                    sum += a[k];
                    mx = Math.max(mx, a[k]);
                }
                ans = Math.max(ans, sum - mx);
            }
        }
        System.out.println(ans);
    }
}',
'java', 'TLE', FALSE);

-- ============================================
-- BÀI 2: Ocean Club
-- ============================================
INSERT INTO Problems (title, description, time_limit, memory_limit, contest_type, checker_script)
VALUES (
    'Ocean Club',
    'Problem B. Ocean Club\nBallon:\nTime limit: 1 seconds\nMemory limit: 512 megabytes\nBi is very fond of the Ocean series on HBO. Because she liked it so much, when she was in her class, she created her own Ocean club. To practice skills like the characters in the movie, Bi gave a math problem to its members practice as follows: Bi''s Ocean club has n members with a list of home address numbers A = {a1,a2,...,an}. The association''s communication rules are from small house address numbers to larger house address numbers and primes together. Bi requires members to calculate quickly to transmit information from a house has an address ai to aj number another house address and must pass through k +1 people, how many ways are there?\nThe members of Bi''s Ocean Club are not good at it yet, so Bi ask you to help calculate.\nInput\nThe first line contains n is the number of members safety 1 <= n <= 100.\nThe second line contains n integers is distinct a1,a2,...,an safety 2 <= ai <= 10^5 - the list address.\nThe third line contains one integer Q safety 1 <= Q <= 10^5 - the number of query Bi will do.\nThe next Q lines contains three integers ai,aj,k safety ai <= aj;ai,aj in A;1 <= k <= n.\nOutput\nOutput Q lines, the i-th line should contain the i-th query result. Since the number of result is large, print it modulo 2023.\nExamples\nstandard input\n7\n2 3 4 5 6 7 8\n1\n2 8 3\nstandard output\n3',
    1000, 512, 'ICPC',
    NULL
);
SET @p2 = LAST_INSERT_ID();

-- Testcases for Ocean Club
INSERT INTO Testcases (problem_id, input_data, expected_output, testcase_type, is_ai_generated) VALUES
(@p2, '5\n2 3 5 7 11\n1\n2 11 2\n', '3\n', 'small', TRUE),
(@p2, '7\n2 3 4 5 6 7 8\n1\n2 8 3\n', '3\n', 'normal', TRUE),
(@p2, '1\n2\n1\n2 2 1\n', '1\n', 'edge', TRUE),
(@p2, '5\n2 3 5 7 11\n1\n2 11 4\n', '1\n', 'anti-wa', TRUE),
(@p2, '5\n2 3 4 5 6\n1\n2 6 3\n', '1\n', 'small', TRUE),
(@p2, '10\n2 3 4 5 6 7 8 9 10 11\n1\n2 11 5\n', '0\n', 'anti-wa', TRUE),
(@p2, '3\n2 3 5\n1\n2 5 1\n', '1\n', 'small', TRUE),
(@p2, '5\n2 3 4 5 6\n1\n2 6 2\n', '2\n', 'anti-wa', TRUE),
(@p2, '2\n2 3\n1\n2 3 1\n', '1\n', 'small', TRUE),
(@p2, '5\n2 3 5 7 11\n2\n2 5 2\n3 7 1\n', '1\n1\n', 'normal', TRUE),
(@p2, '3\n2 3 4\n1\n2 4 2\n', '1\n', 'anti-wa', TRUE),
(@p2, '3\n2 3 5\n1\n2 5 2\n', '1\n', 'anti-wa', TRUE);

-- Sample Codes for Ocean Club
INSERT INTO SampleCodes (problem_id, code_content, language, expected_type, is_ai_generated) VALUES
(@p2,
'import java.util.Scanner;
public class Main {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = sc.nextInt();
        }
        int q = sc.nextInt();
        for (int i = 0; i < q; i++) {
            int ai = sc.nextInt();
            int aj = sc.nextInt();
            int k = sc.nextInt();
            int count = 0;
            for (int j = 0; j < n; j++) {
                if (a[j] > ai && a[j] < aj && isPrime(a[j])) {
                    count++;
                }
            }
            System.out.println(count % 2023);
        }
    }

    public static boolean isPrime(int num) {
        if (num <= 1) return false;
        if (num == 2) return true;
        if (num % 2 == 0) return false;
        for (int i = 3; i * i <= num; i += 2) {
            if (num % i == 0) return false;
        }
        return true;
    }
}',
'java', 'AC', TRUE);

INSERT INTO SampleCodes (problem_id, code_content, language, expected_type, is_ai_generated) VALUES
(@p2,
'import java.util.*;

public class Main {

    static final int MOD = 2023;

    static boolean isPrime(int x) {
        if (x < 2) return false;
        for (int i = 2; i * i <= x; i++)
            if (x % i == 0) return false;
        return true;
    }

    static int C(int n, int k) {
        if (k > n || k < 0) return 0;
        int res = 1;
        for (int i = 1; i <= k; i++) {
            res = res * (n - i + 1) / i;
        }
        return res;
    }

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        int n = sc.nextInt();
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = sc.nextInt();
        Arrays.sort(a);
        int Q = sc.nextInt();
        while (Q-- > 0) {
            int ai = sc.nextInt();
            int aj = sc.nextInt();
            int k = sc.nextInt();
            int l = -1, r = -1;
            for (int i = 0; i < n; i++) {
                if (a[i] == ai) l = i;
                if (a[i] == aj) r = i;
            }
            if (l == -1 || r == -1 || l > r) {
                System.out.println(0);
                continue;
            }
            int count = 0;
            for (int i = l + 1; i < r; i++) {
                if (isPrime(a[i])) count++;
            }
            System.out.println(C(count, k - 1) % MOD);
        }
    }
}',
'java', 'WA', FALSE);
