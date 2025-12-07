# Create Launcher Icons

The easiest way to create launcher icons is using Android Studio's Image Asset Studio:

1. **In Android Studio**: Right-click on `res` folder
2. **Select**: New → Image Asset
3. **Icon Type**: Launcher Icons (Adaptive and Legacy)
4. **Choose**:
   - Clipart: Select a checkmark or task icon
   - Or Text: Type "iDo"
   - Or Image: Upload your own icon
5. **Click Next** → **Finish**

This will automatically generate all required icon sizes and densities.

## Quick Fix (Manual)

Alternatively, use these commands to create temporary placeholder icons:

```bash
cd /Users/sarthakghosh/projects/ido/android/app/src/main/res

# Create simple placeholder PNGs using ImageMagick (if installed)
# Or just download any PNG and name it ic_launcher.png in each folder
```

For now, the app uses vector adaptive icons which should work.
