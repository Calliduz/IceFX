import org.mindrot.jbcrypt.BCrypt;

public class GenerateBCrypt {
    public static void main(String[] args) {
        // Generate hashes for admin passwords
        String password1 = "admin123";
        String password2 = "admin";
        
        String hash1 = BCrypt.hashpw(password1, BCrypt.gensalt(10));
        String hash2 = BCrypt.hashpw(password2, BCrypt.gensalt(10));
        
        System.out.println("========================================");
        System.out.println("BCrypt Password Hashes");
        System.out.println("========================================");
        System.out.println();
        System.out.println("Password: 'admin123'");
        System.out.println("Hash: " + hash1);
        System.out.println();
        System.out.println("Password: 'admin'");
        System.out.println("Hash: " + hash2);
        System.out.println();
        System.out.println("========================================");
        System.out.println("Verification Tests");
        System.out.println("========================================");
        System.out.println("Testing 'admin123' with generated hash: " + BCrypt.checkpw(password1, hash1));
        System.out.println("Testing 'admin' with generated hash: " + BCrypt.checkpw(password2, hash2));
        System.out.println();
        
        // Test the old hashes from SQL file
        String oldHash1 = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhLW";
        String oldHash2 = "$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi";
        
        System.out.println("========================================");
        System.out.println("Testing OLD hashes from SQL file");
        System.out.println("========================================");
        System.out.println("Old hash1 with 'admin123': " + BCrypt.checkpw(password1, oldHash1));
        System.out.println("Old hash2 with 'admin': " + BCrypt.checkpw(password2, oldHash2));
    }
}
