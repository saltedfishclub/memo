package cc.sfclub.sfcraft.memo.auth;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.GameProfileRepository;
import com.mojang.authlib.ProfileLookupCallback;
import com.mojang.authlib.yggdrasil.ProfileNotFoundException;

public class FallbackGameProfileRepository implements GameProfileRepository {
    private final GameProfileRepository[] repositories;

    public FallbackGameProfileRepository(GameProfileRepository... repositories) {
        this.repositories = repositories;
    }

    @Override
    public void findProfilesByNames(String[] names, ProfileLookupCallback originalCallback) {
        findProfiles0(0, names, originalCallback);
    }

    private void findProfiles0(int index, String[] names, ProfileLookupCallback origin) {
        var repo = repositories[index];
        repo.findProfilesByNames(names, new ProfileLookupCallback() {
            @Override
            public void onProfileLookupSucceeded(GameProfile profile) {
                origin.onProfileLookupSucceeded(profile);
            }

            @Override
            public void onProfileLookupFailed(String profileName, Exception exception) {

                if (index == repositories.length - 1 || exception instanceof ProfileNotFoundException) {
                    origin.onProfileLookupFailed(profileName, exception);
                } else {
                    findProfiles0(index + 1, new String[]{profileName}, origin);
                }
            }
        });
    }
}
