def get_latest_tag(tags):
    for tag in reversed(tags):
        tag_name = tag['ref'].replace('refs/tags/', '')
        if tag_name.startswith('v') and tag_name.partition('-')[0].endswith('.0'):
            return tag_name


# latest tag alpha - future version alpha or beta - main branch
# latest tag beta - future version beta or rc - main branch
# latest tag rc - future version rc or stable - release branch
# latest tag stable - future version alpha or beta - main branch
def get_snapshot_branch(latest_tag):
    if 'beta' in latest_tag or 'alpha' in latest_tag \
            or ('rc' not in latest_tag and 'beta' not in latest_tag and 'alpha' not in latest_tag):
        return 'main'
    else:
        return 'release-' + latest_tag.partition('.0')[0]
